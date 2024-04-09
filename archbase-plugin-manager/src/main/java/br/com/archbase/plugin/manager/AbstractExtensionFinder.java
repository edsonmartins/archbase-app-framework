package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.asm.ExtensionInfo;
import br.com.archbase.plugin.manager.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

@SuppressWarnings("java:S3740")
public abstract class AbstractExtensionFinder implements ExtensionFinder, PluginStateListener {

    private static final Logger log = LoggerFactory.getLogger(AbstractExtensionFinder.class);

    protected ArchbasePluginManager archbasePluginManager;
    protected Map<String, Set<String>> entries; // cache by pluginId
    protected Map<String, ExtensionInfo> extensionInfos; // cache extension infos by class name
    protected Boolean checkForExtensionDependencies = null;

    protected AbstractExtensionFinder(ArchbasePluginManager archbasePluginManager) {
        this.archbasePluginManager = archbasePluginManager;
    }

    public static Extension findExtensionAnnotation(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Extension.class)) {
            return clazz.getAnnotation(Extension.class);
        }

        // pesquisa recursivamente em todas as anotações
        for (Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> annotationClass = annotation.annotationType();
            if (!annotationClass.getName().startsWith("java.lang.annotation")) {
                Extension extensionAnnotation = findExtensionAnnotation(annotationClass);
                if (extensionAnnotation != null) {
                    return extensionAnnotation;
                }
            }
        }

        return null;
    }

    public abstract Map<String, Set<String>> readPluginsStorages();

    public abstract Map<String, Set<String>> readClasspathStorages();

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<ExtensionWrapper<T>> find(Class<T> type) {
        log.debug("Encontrar extensões de ponto de extensão '{}'", type.getName());
        Map<String, Set<String>> entriesSetMap = getEntries();
        List<ExtensionWrapper<T>> result = new ArrayList<>();

        // adicionar extensões encontradas no classpath e plug-ins
        for (String pluginId : entriesSetMap.keySet()) {
            // extensões do classpath <=> pluginId = null
            List<ExtensionWrapper<T>> pluginExtensions = find(type, pluginId);
            result.addAll(pluginExtensions);
        }

        if (result.isEmpty()) {
            log.debug("Nenhuma extensão encontrada para o ponto de extensão '{}'", type.getName());
        } else {
            log.debug("Foram encontradas {} extensões para o ponto de extensão '{}'", result.size(), type.getName());
        }

        // classificar por propriedade "ordinal"
        Collections.sort(result);

        return result;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public <T> List<ExtensionWrapper<T>> find(Class<T> type, String pluginId) {
        log.debug("Encontrando extensões do ponto de extensão '{}' para o plug-in '{}'", type.getName(), pluginId);
        List<ExtensionWrapper<T>> result = new ArrayList<>();

        // extensões do classpath <=> pluginId = null
        Set<String> classNames = findClassNames(pluginId);
        if (classNames == null || classNames.isEmpty()) {
            return result;
        }

        if (pluginId != null) {
            PluginWrapper pluginWrapper = archbasePluginManager.getPlugin(pluginId);
            if (PluginState.STARTED != pluginWrapper.getPluginState()) {
                return result;
            }

            log.trace("Verificando extensões do archbasePlugin '{}'", pluginId);
        } else {
            log.trace("Verificando extensões do classpath");
        }

        ClassLoader classLoader = (pluginId != null) ? archbasePluginManager.getPluginClassLoader(pluginId) : getClass().getClassLoader();

        boolean ignore = false;

        for (String className : classNames) {
            ignore = false;
            try {
                if (isCheckForExtensionDependencies()) {
                    // Carrega a anotação de extensão sem inicializar a própria classe.
                    //
                    // Se dependências opcionais forem usadas, o carregador de classes pode não ser capaz
                    // para carregar a classe de extensão devido à falta de dependências opcionais.
                    //
                    // Portanto, estamos extraindo a anotação de extensão via asm, para
                    // para extrair os plug-ins necessários para uma extensão. Somente se tudo for necessário
                    // plugins estão atualmente disponíveis e iniciados, o correspondente
                    // extensão é carregada por meio do carregador de classes.
                    ExtensionInfo extensionInfo = getExtensionInfo(className, classLoader);
                    if (extensionInfo == null) {
                        log.error("Nenhuma anotação de extensão foi encontrada para '{}'", className);
                        ignore = true;
                    } else {
                        // Certifique-se de que todos os plug-ins exigidos por esta extensão estão disponíveis.
                        List<String> missingPluginIds = new ArrayList<>();
                        for (String requiredPluginId : extensionInfo.getPlugins()) {
                            PluginWrapper requiredPlugin = archbasePluginManager.getPlugin(requiredPluginId);
                            if (requiredPlugin == null || !PluginState.STARTED.equals(requiredPlugin.getPluginState())) {
                                missingPluginIds.add(requiredPluginId);
                            }
                        }
                        if (!missingPluginIds.isEmpty()) {
                            StringBuilder missing = new StringBuilder();
                            for (String missingPluginId : missingPluginIds) {
                                if (missing.length() > 0) missing.append(", ");
                                missing.append(missingPluginId);
                            }
                            log.trace("A extensão '{}' foi ignorada devido à falta de plug-ins: {}", className, missing);
                            ignore = true;
                        }
                    }
                }

                if (!ignore) {
                    log.debug("Carregando classe '{}' usando o carregador de classe '{}'", className, classLoader);
                    Class<?> extensionClass = classLoader.loadClass(className);

                    log.debug("Verificando o tipo de extensão '{}'", className);
                    if (type.isAssignableFrom(extensionClass)) {
                        ExtensionWrapper extensionWrapper = createExtensionWrapper(extensionClass);
                        result.add(extensionWrapper);
                        log.debug("Adicionada extensão '{}' com ordinal {}", className, extensionWrapper.getOrdinal());
                    } else {
                        log.trace("'{} 'não é uma extensão para o ponto de extensão' {}'", className, type.getName());
                        if (RuntimeMode.DEVELOPMENT.equals(archbasePluginManager.getRuntimeMode())) {
                            checkDifferentClassLoaders(type, extensionClass);
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (result.isEmpty()) {
            log.debug("No extensions found for extension point '{}'", type.getName());
        } else {
            log.debug("Encontrou {} extensões para o ponto de extensão '{}'", result.size(), type.getName());
        }

        // classificar por propriedade "ordinal"
        Collections.sort(result);

        return result;
    }

    @Override
    public List<ExtensionWrapper> find(String pluginId) {
        log.debug("Encontrar extensões do archbasePlugin '{}'", pluginId);
        List<ExtensionWrapper> result = new ArrayList<>();

        Set<String> classNames = findClassNames(pluginId);
        if (classNames.isEmpty()) {
            return result;
        }

        if (pluginId != null) {
            PluginWrapper pluginWrapper = archbasePluginManager.getPlugin(pluginId);
            if (PluginState.STARTED != pluginWrapper.getPluginState()) {
                return result;
            }

            log.trace("Verificando extensões do archbasePlugin '{}'", pluginId);
        } else {
            log.trace("Verificando extensões do classpath");
        }

        ClassLoader classLoader = (pluginId != null) ? archbasePluginManager.getPluginClassLoader(pluginId) : getClass().getClassLoader();

        for (String className : classNames) {
            try {
                log.debug("Carregando classe '{}' usando o carregador de classe '{}'", className, classLoader);
                Class<?> extensionClass = classLoader.loadClass(className);

                ExtensionWrapper extensionWrapper = createExtensionWrapper(extensionClass);
                result.add(extensionWrapper);
                log.debug("Adicionada extensão '{}' com ordinal {}", className, extensionWrapper.getOrdinal());
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.error(e.getMessage(), e);
            }
        }

        if (result.isEmpty()) {
            log.debug("Nenhuma extensão encontrada para o archbasePlugin '{}'", pluginId);
        } else {
            log.debug("Foram encontradas {} extensões para plug-in '{}'", result.size(), pluginId);
        }

        // classificar por propriedade "ordinal"
        Collections.sort(result);

        return result;
    }

    @Override
    public Set<String> findClassNames(String pluginId) {
        return getEntries().get(pluginId);
    }

    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        // TODO otimizar (faça apenas para algumas transições)
        // clear cache
        entries = null;

        // Por padrão, estamos assumindo que nenhuma verificação de dependências de extensão é necessária.
        //
        // Um plug-in, que tem uma dependência opcional de outros plug-ins, pode levar a não carregar
        // Classes Java (NoClassDefFoundError) no tempo de execução do aplicativo devido à possível ausência
        // das dependências. Portanto, estamos ativando a verificação de extensões opcionais, se o
        // archbasePlugin iniciado contém pelo menos uma dependência de archbasePlugin opcional.
        if (checkForExtensionDependencies == null && PluginState.STARTED.equals(event.getPluginState())) {
            for (PluginDependency dependency : event.getPlugin().getDescriptor().getDependencies()) {
                if (dependency.isOptional()) {
                    log.debug("Habilitar verificação de dependências de extensão via ASM.");
                    checkForExtensionDependencies = true;
                    break;
                }
            }
        }
    }

    /**
     * Retorna verdadeiro, se o localizador de extensões verificar as extensões de seus plug-ins necessários.
     * Este recurso deve ser habilitado, para verificar a disponibilidade de
     * {@link Extension#plugins()} configurado por uma extensão.
     * <p>
     * Este recurso é habilitado por padrão, se pelo menos um archbasePlugin disponível faz uso de
     * dependências opcionais do archbasePlugin. Esses plug-ins opcionais podem não estar disponíveis no tempo de execução.
     * Portanto, qualquer extensão é verificada por padrão em relação aos plug-ins disponíveis antes de seu
     * instanciação.
     * <p>
     * Aviso: este recurso requer a <a href="https://asm.ow2.io/">biblioteca ASM</a> opcional
     * para estar disponível no classpath dos aplicativos.
     *
     * @return true, se o localizador de extensão verificar as extensões para seus plug-ins necessários
     */
    public final boolean isCheckForExtensionDependencies() {
        return Boolean.TRUE.equals(checkForExtensionDependencies);
    }

    /**
     * Os desenvolvedores de plug-ins podem ativar / desativar as verificações dos plug-ins necessários de uma extensão.
     * Este recurso deve ser habilitado, para verificar a disponibilidade de
     * {@link Extension#plugins()} configurado por uma extensão.
     * <p>
     * Este recurso é habilitado por padrão, se pelo menos um archbasePlugin disponível faz uso de
     * dependências opcionais do archbasePlugin. Esses plug-ins opcionais podem não estar disponíveis no tempo de execução.
     * Portanto, qualquer extensão é verificada por padrão em relação aos plug-ins disponíveis antes de seu
     * instanciação.
     * <p>
     * Aviso: este recurso requer a <a href="https://asm.ow2.io/">biblioteca ASM</a> opcional
     * para estar disponível no classpath dos aplicativos.
     *
     * @param checkForExtensionDependencies true para habilitar verificações de extensões opcionais, caso contrário, false
     */
    public void setCheckForExtensionDependencies(boolean checkForExtensionDependencies) {
        this.checkForExtensionDependencies = checkForExtensionDependencies;
    }

    protected void debugExtensions(Set<String> extensions) {
        if (log.isDebugEnabled()) {
            if (extensions.isEmpty()) {
                log.debug("Nenhuma extensão encontrada");
            } else {
                log.debug("Foram encontradas {} extensões possíveis:", extensions.size());
                for (String extension : extensions) {
                    String message = "   " + extension;
                    log.debug(message);
                }
            }
        }
    }

    private Map<String, Set<String>> readStorages() {
        Map<String, Set<String>> result = new LinkedHashMap<>();

        result.putAll(readClasspathStorages());
        result.putAll(readPluginsStorages());

        return result;
    }

    private Map<String, Set<String>> getEntries() {
        if (entries == null) {
            entries = readStorages();
        }

        return entries;
    }

    /**
     * Retorna os parâmetros de uma anotação {@link Extension} sem carregar
     * a classe correspondente no carregador de classes.
     *
     * @param className   nome da classe, que contém a anotação {@link Extension} solicitada
     * @param classLoader carregador de classes para acessar a classe
     * @return o conteúdo da anotação {@link Extension} ou null, se a classe não
     * tem uma anotação {@link Extension}
     */
    private ExtensionInfo getExtensionInfo(String className, ClassLoader classLoader) {
        if (extensionInfos == null) {
            extensionInfos = new HashMap<>();
        }

        if (!extensionInfos.containsKey(className)) {
            log.trace("Carregar anotação para '{}' usando asm", className);
            ExtensionInfo info = ExtensionInfo.load(className, classLoader);
            if (info == null) {
                log.warn("Nenhuma anotação de extensão foi encontrada para '{}'", className);
                extensionInfos.put(className, null);
            } else {
                extensionInfos.put(className, info);
            }
        }

        return extensionInfos.get(className);
    }

    private ExtensionWrapper createExtensionWrapper(Class<?> extensionClass) {
        Extension extensionAnnotation = findExtensionAnnotation(extensionClass);
        int ordinal = extensionAnnotation != null ? extensionAnnotation.ordinal() : 0;
        ExtensionDescriptor descriptor = new ExtensionDescriptor(ordinal, extensionClass);

        return new ExtensionWrapper<>(descriptor, archbasePluginManager.getExtensionFactory());
    }

    private void checkDifferentClassLoaders(Class<?> type, Class<?> extensionClass) {
        ClassLoader typeClassLoader = type.getClassLoader(); // class loader of extension point
        ClassLoader extensionClassLoader = extensionClass.getClassLoader();
        boolean match = ClassUtils.getAllInterfacesNames(extensionClass).contains(type.getSimpleName());
        if (match && !extensionClassLoader.equals(typeClassLoader)) {
            // neste cenário, o método 'isAssignableFrom' retorna apenas FALSE
            // consulte http://www.coderanch.com/t/557846/java/java/FWIW-FYI-isAssignableFrom-isInstance-differing
            log.error("Carregadores de classes diferentes: '{}' (E) e '{}' (EP)", extensionClassLoader, typeClassLoader);
        }
    }

}
