package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Esta classe implementa o código do plug-in padrão que qualquer {@link ArchbasePluginManager}
 * a implementação teria que apoiar.
 * Ajuda a eliminar o ruído da subclasse que lida com o gerenciamento de plug-ins.
 *
 * <p> Esta classe não é thread-safe.
 */
public abstract class AbstractArchbasePluginManager implements ArchbasePluginManager {

    public static final String PLUGINS_DIR_PROPERTY_NAME = "archbase.pluginsDir";
    public static final String MODE_PROPERTY_NAME = "archbase.plugin.mode";
    public static final String DEFAULT_PLUGINS_DIR = "plugins";
    public static final String DEVELOPMENT_PLUGINS_DIR = "../plugins";
    public static final String ZERO_VERSION = "0.0.0";
    private static final Logger log = LoggerFactory.getLogger(AbstractArchbasePluginManager.class);
    protected final List<Path> pluginsRoots = new ArrayList<>();

    protected ExtensionFinder extensionFinder;

    protected PluginDescriptorFinder pluginDescriptorFinder;

    /**
     * Um mapa de plug-ins pelos quais este gerenciador é responsável (a chave é o 'pluginId').
     */
    protected Map<String, PluginWrapper> plugins;

    /**
     * Um mapa de carregadores de classe de archbasePlugin (a chave é o 'pluginId').
     */
    protected Map<String, ClassLoader> pluginClassLoaders;

    /**
     * Uma lista com plug-ins não resolvidos (dependência não resolvida).
     */
    protected List<PluginWrapper> unresolvedPlugins;

    /**
     * Uma lista com todos os plug-ins resolvidos (dependência resolvida).
     */
    protected List<PluginWrapper> resolvedPlugins;

    /**
     * Uma lista com plug-ins iniciados.
     */
    protected List<PluginWrapper> startedPlugins;

    /**
     * Os {@link PluginStateListener}s registrados.
     */
    protected List<PluginStateListener> pluginStateListeners;

    /**
     * Valor do cache para o modo runtime.
     * Não há necessidade de relê-lo porque não mudará em tempo de execução.
     */
    protected RuntimeMode runtimeMode;

    /**
     * A versão do sistema usada para comparações com o archbasePlugin requer o atributo.
     */
    protected String systemVersion = ZERO_VERSION;

    protected PluginRepository pluginRepository;
    protected ArchbasePluginFactory archbasePluginFactory;
    protected ExtensionFactory extensionFactory;
    protected PluginStatusProvider pluginStatusProvider;
    protected DependencyResolver dependencyResolver;
    protected ArchbasePluginLoader archbasePluginLoader;
    protected boolean exactVersionAllowed = false;

    protected VersionManager versionManager;

    /**
     * As raízes dos plug-ins são fornecidas como uma lista separada por vírgulas por {@code System.getProperty ("archbase.pluginsDir", "plugins")}.
     */
    protected AbstractArchbasePluginManager() {
        initialize();
    }

    /**
     * Constrói {@code AbstractArchbasePluginManager} com as raízes de plug-ins fornecidas.
     *
     * @param pluginsRoots as raízes para procurar plug-ins
     */
    protected AbstractArchbasePluginManager(Path... pluginsRoots) {
        this(Arrays.asList(pluginsRoots));
    }

    /**
     * Constrói {@code AbstractArchbasePluginManager} com as raízes de plug-ins fornecidas.
     *
     * @param pluginsRoots as raízes para procurar plug-ins
     */
    protected AbstractArchbasePluginManager(List<Path> pluginsRoots) {
        this.pluginsRoots.addAll(pluginsRoots);

        initialize();
    }

    @Override
    public String getSystemVersion() {
        return systemVersion;
    }

    @Override
    public void setSystemVersion(String version) {
        systemVersion = version;
    }

    /**
     * Retorna uma cópia dos plug-ins.
     */
    @Override
    public List<PluginWrapper> getPlugins() {
        return new ArrayList<>(plugins.values());
    }

    /**
     * Retorna uma cópia dos plug-ins com esse estado.
     */
    @Override
    public List<PluginWrapper> getPlugins(PluginState pluginState) {
        List<PluginWrapper> pluginWrapperList = new ArrayList<>();
        for (PluginWrapper plugin : getPlugins()) {
            if (pluginState.equals(plugin.getPluginState())) {
                pluginWrapperList.add(plugin);
            }
        }

        return pluginWrapperList;
    }

    @Override
    public List<PluginWrapper> getResolvedPlugins() {
        return resolvedPlugins;
    }

    @Override
    public List<PluginWrapper> getUnresolvedPlugins() {
        return unresolvedPlugins;
    }

    @Override
    public List<PluginWrapper> getStartedPlugins() {
        return startedPlugins;
    }

    @Override
    public PluginWrapper getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    @Override
    public String loadPlugin(Path pluginPath) {
        if ((pluginPath == null) || Files.notExists(pluginPath)) {
            throw new IllegalArgumentException(String.format("O archbasePlugin especificado% s não existe!", pluginPath));
        }

        log.debug("Carregando archbasePlugin de '{}'", pluginPath);

        PluginWrapper pluginWrapper = loadPluginFromPath(pluginPath);

        // tente resolver o archbasePlugin carregado junto com outros plugins possíveis que dependem deste archbasePlugin
        resolvePlugins();

        return pluginWrapper.getDescriptor().getPluginId();
    }

    /**
     * Carregar plugins.
     */
    @Override
    public void loadPlugins() {
        log.debug("Plug-ins de pesquisa em '{}'", pluginsRoots);
        // verificar raízes de plug-ins
        if (pluginsRoots.isEmpty()) {
            log.warn("Nenhuma raiz de plug-ins configurada");
            return;
        }
        pluginsRoots.forEach(path -> {
            if (Files.notExists(path) || !Files.isDirectory(path)) {
                log.warn("Sem '{}' root", path);
            }
        });

        // obter todos os caminhos do archbasePlugin do repositório
        List<Path> pluginPaths = pluginRepository.getPluginPaths();

        // verifique se não há plug-ins
        if (pluginPaths.isEmpty()) {
            log.info("Sem plugins");
            return;
        }

        log.debug("Encontrados {} possíveis plug-ins: {}", pluginPaths.size(), pluginPaths);

        // carregar plugins de caminhos de plugins
        for (Path pluginPath : pluginPaths) {
            try {
                loadPluginFromPath(pluginPath);
            } catch (PluginRuntimeException e) {
                log.error(e.getMessage(), e);
            }
        }

        // resolver plugins
        try {
            resolvePlugins();
        } catch (PluginRuntimeException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Descarregue todos os plug-ins
     */
    @Override
    public void unloadPlugins() {
        // agrupe os resolvidos em uma nova lista devido à modificação simultânea
        for (PluginWrapper pluginWrapper : new ArrayList<>(resolvedPlugins)) {
            unloadPlugin(pluginWrapper.getPluginId());
        }
    }

    /**
     * Descarregue o archbasePlugin especificado e seus dependentes.
     */
    @Override
    public boolean unloadPlugin(String pluginId) {
        return unloadPlugin(pluginId, true);
    }

    protected boolean unloadPlugin(String pluginId, boolean unloadDependents) {
        try {
            if (unloadDependents) {
                List<String> dependents = dependencyResolver.getDependents(pluginId);
                while (!dependents.isEmpty()) {
                    String dependent = dependents.remove(0);
                    unloadPlugin(dependent, false);
                    dependents.addAll(0, dependencyResolver.getDependents(dependent));
                }
            }

            PluginState pluginState = stopPlugin(pluginId, false);
            if (PluginState.STARTED == pluginState) {
                return false;
            }

            PluginWrapper pluginWrapper = getPlugin(pluginId);
            log.info("Descarregar archbasePlugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));

            // remove the archbasePlugin
            plugins.remove(pluginId);
            getResolvedPlugins().remove(pluginWrapper);

            firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));

            // remove the classloader
            Map<String, ClassLoader> plugClassLoaders = getPluginClassLoaders();
            if (plugClassLoaders.containsKey(pluginId)) {
                closeClassLoader(pluginId, plugClassLoaders);
            }

            return true;
        } catch (IllegalArgumentException e) {
            // ignorar exceções não encontradas porque este método é recursivo
        }

        return false;
    }

    private void closeClassLoader(String pluginId, Map<String, ClassLoader> plugClassLoaders) {
        ClassLoader classLoader = plugClassLoaders.remove(pluginId);
        if (classLoader instanceof Closeable) {
            try {
                ((Closeable) classLoader).close();
            } catch (IOException e) {
                throw new PluginRuntimeException(e, "Não é possível fechar o carregador de classe");
            }
        }
    }

    @Override
    public boolean deletePlugin(String pluginId) {
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        // pare o archbasePlugin se ele for iniciado
        PluginState pluginState = stopPlugin(pluginId);
        if (PluginState.STARTED == pluginState) {
            log.error("Falha ao parar o archbasePlugin '{}' na exclusão", pluginId);
            return false;
        }

        // obtém uma instância do archbasePlugin antes que o archbasePlugin seja descarregado
        // para ver o motivo, consulte https://github.com/pf4j/pf4j/issues/309
        ArchbasePlugin archbasePlugin = pluginWrapper.getPlugin();

        if (!unloadPlugin(pluginId)) {
            log.error("Falha ao descarregar o archbasePlugin '{}' na exclusão", pluginId);
            return false;
        }

        // notifica o archbasePlugin quando ele é excluído
        archbasePlugin.delete();

        Path pluginPath = pluginWrapper.getPluginPath();

        return pluginRepository.deletePluginPath(pluginPath);
    }

    /**
     * Inicie todos os plug-ins ativos.
     */
    @Override
    public void startPlugins() {
        for (PluginWrapper pluginWrapper : resolvedPlugins) {
            PluginState pluginState = pluginWrapper.getPluginState();
            if ((PluginState.DISABLED != pluginState) && (PluginState.STARTED != pluginState)) {
                try {
                    log.info("Iniciar archbasePlugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
                    pluginWrapper.getPlugin().start();
                    pluginWrapper.setPluginState(PluginState.STARTED);
                    pluginWrapper.setFailedException(null);
                    startedPlugins.add(pluginWrapper);
                } catch (Exception | LinkageError e) {
                    pluginWrapper.setPluginState(PluginState.FAILED);
                    pluginWrapper.setFailedException(e);
                    log.error("Não foi possível iniciar o archbasePlugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()), e);
                } finally {
                    firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
                }
            }
        }
    }

    /**
     * Inicie o plug-in especificado e suas dependências.
     */
    @Override
    public PluginState startPlugin(String pluginId) {
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.STARTED == pluginState) {
            log.debug("ArchbasePlugin já iniciado '{}'", getPluginLabel(pluginDescriptor));
            return PluginState.STARTED;
        }

        if (!resolvedPlugins.contains(pluginWrapper)) {
            log.warn("Não é possível iniciar um archbasePlugin não resolvido '{}'", getPluginLabel(pluginDescriptor));
            return pluginState;
        }

        // ativar o archbasePlugin automaticamente no início manual do archbasePlugin
        if (PluginState.DISABLED == pluginState && !enablePlugin(pluginId)) {
            return pluginState;
        }

        for (PluginDependency dependency : pluginDescriptor.getDependencies()) {
            // iniciar a dependência apenas se marcada como necessária (não opcional) ou se for opcional e carregada
            if (!dependency.isOptional() || plugins.containsKey(dependency.getPluginId())) {
                startPlugin(dependency.getPluginId());
            }
        }

        log.info("Iniciar archbasePlugin '{}'", getPluginLabel(pluginDescriptor));
        pluginWrapper.getPlugin().start();
        pluginWrapper.setPluginState(PluginState.STARTED);
        startedPlugins.add(pluginWrapper);

        firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));

        return pluginWrapper.getPluginState();
    }

    /**
     * Pare todos os plug-ins ativos.
     */
    @Override
    public void stopPlugins() {
        // pare os plug-ins iniciados na ordem inversa
        Collections.reverse(startedPlugins);
        Iterator<PluginWrapper> itr = startedPlugins.iterator();
        while (itr.hasNext()) {
            PluginWrapper pluginWrapper = itr.next();
            PluginState pluginState = pluginWrapper.getPluginState();
            if (PluginState.STARTED == pluginState) {
                try {
                    log.info("Parar o archbasePlugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
                    pluginWrapper.getPlugin().stop();
                    pluginWrapper.setPluginState(PluginState.STOPPED);
                    itr.remove();

                    firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
                } catch (PluginRuntimeException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Pare o archbasePlugin especificado e seus dependentes.
     */
    @Override
    public PluginState stopPlugin(String pluginId) {
        return stopPlugin(pluginId, true);
    }

    protected PluginState stopPlugin(String pluginId, boolean stopDependents) {
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.STOPPED == pluginState) {
            log.debug("ArchbasePlugin já parado '{}'", getPluginLabel(pluginDescriptor));
            return PluginState.STOPPED;
        }

        // teste para archbasePlugin desativado
        if (PluginState.DISABLED == pluginState) {
            // fazer nada
            return pluginState;
        }

        if (stopDependents) {
            List<String> dependents = dependencyResolver.getDependents(pluginId);
            while (!dependents.isEmpty()) {
                String dependent = dependents.remove(0);
                stopPlugin(dependent, false);
                dependents.addAll(0, dependencyResolver.getDependents(dependent));
            }
        }

        log.info("Parar o archbasePlugin '{}'", getPluginLabel(pluginDescriptor));
        pluginWrapper.getPlugin().stop();
        pluginWrapper.setPluginState(PluginState.STOPPED);
        startedPlugins.remove(pluginWrapper);

        firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));

        return pluginWrapper.getPluginState();
    }

    protected void checkPluginId(String pluginId) {
        if (!plugins.containsKey(pluginId)) {
            throw new IllegalArgumentException(String.format("PluginId desconhecido %s", pluginId));
        }
    }

    @Override
    public boolean disablePlugin(String pluginId) {
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.DISABLED == pluginState) {
            log.debug("ArchbasePlugin já desativado '{}'", getPluginLabel(pluginDescriptor));
            return true;
        }

        if (PluginState.STOPPED == stopPlugin(pluginId)) {
            pluginWrapper.setPluginState(PluginState.DISABLED);

            firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, PluginState.STOPPED));

            pluginStatusProvider.disablePlugin(pluginId);
            log.info("Plug-in desativado '{}'", getPluginLabel(pluginDescriptor));

            return true;
        }

        return false;
    }

    @Override
    public boolean enablePlugin(String pluginId) {
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        if (!isPluginValid(pluginWrapper)) {
            log.warn("ArchbasePlugin '{}' não pode ser habilitado", getPluginLabel(pluginWrapper.getDescriptor()));
            return false;
        }

        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.DISABLED != pluginState) {
            log.debug("ArchbasePlugin '{}' não está desabilitado", getPluginLabel(pluginDescriptor));
            return true;
        }

        pluginStatusProvider.enablePlugin(pluginId);

        pluginWrapper.setPluginState(PluginState.CREATED);

        firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));

        log.info("Plug-in ativado '{}'", getPluginLabel(pluginDescriptor));

        return true;
    }

    /**
     * Obtenha o {@link ClassLoader} para plug-in.
     */
    @Override
    public ClassLoader getPluginClassLoader(String pluginId) {
        return pluginClassLoaders.get(pluginId);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<Class<?>> getExtensionClasses(String pluginId) {
        List<ExtensionWrapper> extensionsWrapper = extensionFinder.find(pluginId);
        List<Class<?>> extensionClasses = new ArrayList<>(extensionsWrapper.size());
        for (ExtensionWrapper extensionWrapper : extensionsWrapper) {
            Class<?> c = extensionWrapper.getDescriptor().extensionClass;
            extensionClasses.add(c);
        }

        return extensionClasses;
    }

    @Override
    public <T> List<Class<? extends T>> getExtensionClasses(Class<T> type) {
        return getExtensionClasses(extensionFinder.find(type));
    }

    @Override
    public <T> List<Class<? extends T>> getExtensionClasses(Class<T> type, String pluginId) {
        return getExtensionClasses(extensionFinder.find(type, pluginId));
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type) {
        return getExtensions(extensionFinder.find(type));
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type, String pluginId) {
        return getExtensions(extensionFinder.find(type, pluginId));
    }

    @Override
    @SuppressWarnings("java:S3740")
    public List getExtensions(String pluginId) {
        List<ExtensionWrapper> extensionsWrapper = extensionFinder.find(pluginId);
        List extensions = new ArrayList<>(extensionsWrapper.size());
        for (ExtensionWrapper extensionWrapper : extensionsWrapper) {
            try {
                extensions.add(extensionWrapper.getExtension());
            } catch (PluginRuntimeException e) {
                log.error("Não é possível recuperar a extensão", e);
            }
        }

        return extensions;
    }

    @Override
    public Set<String> getExtensionClassNames(String pluginId) {
        return extensionFinder.findClassNames(pluginId);
    }

    @Override
    public ExtensionFactory getExtensionFactory() {
        return extensionFactory;
    }

    public ArchbasePluginLoader getPluginLoader() {
        return archbasePluginLoader;
    }

    public List<Path> getPluginsRoots() {
        return Collections.unmodifiableList(pluginsRoots);
    }

    @Override
    public Path getPluginsRoot() {
        return pluginsRoots.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("pluginsRoots ainda não foi inicializado."));
    }

    @Override
    public RuntimeMode getRuntimeMode() {
        if (runtimeMode == null) {
            // recupera o modo de tempo de execução do sistema
            String modeAsString = System.getProperty(MODE_PROPERTY_NAME, RuntimeMode.DEPLOYMENT.toString());
            runtimeMode = RuntimeMode.byName(modeAsString);
        }

        return runtimeMode;
    }

    @Override
    public PluginWrapper whichPlugin(Class<?> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        for (PluginWrapper plugin : resolvedPlugins) {
            if (plugin.getPluginClassLoader() == classLoader) {
                return plugin;
            }
        }

        return null;
    }

    @Override
    public synchronized void addPluginStateListener(PluginStateListener listener) {
        pluginStateListeners.add(listener);
    }

    @Override
    public synchronized void removePluginStateListener(PluginStateListener listener) {
        pluginStateListeners.remove(listener);
    }

    public String getVersion() {
        String version = null;

        Package pf4jPackage = ArchbasePluginManager.class.getPackage();
        if (pf4jPackage != null) {
            version = pf4jPackage.getImplementationVersion();
            if (version == null) {
                version = pf4jPackage.getSpecificationVersion();
            }
        }

        return (version != null) ? version : ZERO_VERSION;
    }

    protected abstract PluginRepository createPluginRepository();

    protected abstract ArchbasePluginFactory createPluginFactory();

    protected abstract ExtensionFactory createExtensionFactory();

    protected abstract PluginDescriptorFinder createPluginDescriptorFinder();

    protected abstract ExtensionFinder createExtensionFinder();

    protected abstract PluginStatusProvider createPluginStatusProvider();

    protected abstract ArchbasePluginLoader createPluginLoader();

    protected abstract VersionManager createVersionManager();

    protected PluginDescriptorFinder getPluginDescriptorFinder() {
        return pluginDescriptorFinder;
    }

    protected ArchbasePluginFactory getPluginFactory() {
        return archbasePluginFactory;
    }

    protected Map<String, ClassLoader> getPluginClassLoaders() {
        return pluginClassLoaders;
    }

    protected void initialize() {
        plugins = new HashMap<>();
        pluginClassLoaders = new HashMap<>();
        unresolvedPlugins = new ArrayList<>();
        resolvedPlugins = new ArrayList<>();
        startedPlugins = new ArrayList<>();

        pluginStateListeners = new ArrayList<>();

        if (pluginsRoots.isEmpty()) {
            pluginsRoots.addAll(createPluginsRoot());
        }

        pluginRepository = createPluginRepository();
        archbasePluginFactory = createPluginFactory();
        extensionFactory = createExtensionFactory();
        pluginDescriptorFinder = createPluginDescriptorFinder();
        extensionFinder = createExtensionFinder();
        pluginStatusProvider = createPluginStatusProvider();
        archbasePluginLoader = createPluginLoader();

        versionManager = createVersionManager();
        dependencyResolver = new DependencyResolver(versionManager);
    }

    /**
     * Adicione a possibilidade de substituir as raízes dos plug-ins.
     * Se uma propriedade de sistema {@link #PLUGINS_DIR_PROPERTY_NAME} for definida, esse método retornará essas raízes.
     * Se {@link #getRuntimeMode()} retornar {@link RuntimeMode#DEVELOPMENT}, então {@link #DEVELOPMENT_PLUGINS_DIR}
     * é retornado, caso contrário, este método retorna {@link #DEFAULT_PLUGINS_DIR}.
     *
     * @return a raiz dos plug-ins
     */
    protected List<Path> createPluginsRoot() {
        String pluginsDir = System.getProperty(PLUGINS_DIR_PROPERTY_NAME);
        if (pluginsDir != null && !pluginsDir.isEmpty()) {
            return Arrays.stream(pluginsDir.split(","))
                    .map(String::trim)
                    .map(Paths::get)
                    .collect(Collectors.toList());
        }

        pluginsDir = isDevelopment() ? DEVELOPMENT_PLUGINS_DIR : DEFAULT_PLUGINS_DIR;
        return Collections.singletonList(Paths.get(pluginsDir));
    }

    /**
     * Verifique se este archbasePlugin é válido (satisfaz o parâmetro "requer") para uma determinada versão do sistema.
     *
     * @param pluginWrapper o archbasePlugin para verificar
     * @return true se o archbasePlugin satisfaz os "requer" ou se o requer foi deixado em branco
     */
    protected boolean isPluginValid(PluginWrapper pluginWrapper) {
        String requires = pluginWrapper.getDescriptor().getRequires().trim();
        if (!isExactVersionAllowed() && requires.matches("^\\d+\\.\\d+\\.\\d+$")) {
            // Se versões exatas não são permitidas em requer, reescrever para> = expressão
            requires = ">=" + requires;
        }
        if (systemVersion.equals(ZERO_VERSION) || versionManager.checkVersionConstraint(systemVersion, requires)) {
            return true;
        }

        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        log.warn("O plug-in '{}' requer uma versão mínima do sistema de {}, e você tem {}",
                getPluginLabel(pluginDescriptor),
                requires,
                getSystemVersion());

        return false;
    }

    protected boolean isPluginDisabled(String pluginId) {
        return pluginStatusProvider.isPluginDisabled(pluginId);
    }

    protected void resolvePlugins() {
        // recupera os descritores de plugins
        List<PluginDescriptor> descriptors = new ArrayList<>();
        for (PluginWrapper plugin : plugins.values()) {
            descriptors.add(plugin.getDescriptor());
        }

        DependencyResolver.Result result = dependencyResolver.resolve(descriptors);

        if (result.hasCyclicDependency()) {
            throw new DependencyResolver.CyclicDependencyException();
        }

        List<String> notFoundDependencies = result.getNotFoundDependencies();
        if (!notFoundDependencies.isEmpty()) {
            throw new DependencyResolver.DependenciesNotFoundException(notFoundDependencies);
        }

        List<DependencyResolver.WrongDependencyVersion> wrongVersionDependencies = result.getWrongVersionDependencies();
        if (!wrongVersionDependencies.isEmpty()) {
            throw new DependencyResolver.DependenciesWrongVersionException(wrongVersionDependencies);
        }

        List<String> sortedPlugins = result.getSortedPlugins();

        // mover plugins de "não resolvido" para "resolvido"
        for (String pluginId : sortedPlugins) {
            PluginWrapper pluginWrapper = plugins.get(pluginId);
            if (unresolvedPlugins.remove(pluginWrapper)) {
                PluginState pluginState = pluginWrapper.getPluginState();
                if (pluginState != PluginState.DISABLED) {
                    pluginWrapper.setPluginState(PluginState.RESOLVED);
                }

                resolvedPlugins.add(pluginWrapper);
                log.info("ArchbasePlugin '{}' resolvido", getPluginLabel(pluginWrapper.getDescriptor()));

                firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
            }
        }
    }

    protected synchronized void firePluginStateEvent(PluginStateEvent event) {
        for (PluginStateListener listener : pluginStateListeners) {
            log.trace("Dispare '{}' para '{}'", event, listener);
            listener.pluginStateChanged(event);
        }
    }

    protected PluginWrapper loadPluginFromPath(Path pluginPath) {
        // Teste para duplicação do caminho do archbasePlugin
        String pluginId = idForPath(pluginPath);
        if (pluginId != null) {
            throw new PluginAlreadyLoadedException(pluginId, pluginPath);
        }

        // Recupere e valide o descritor do archbasePlugin
        PluginDescriptorFinder descriptorFinder = getPluginDescriptorFinder();
        log.debug("Use '{}' para encontrar descritores de plug-ins", descriptorFinder);
        log.debug("Localizando descritor de archbasePlugin para archbasePlugin '{}'", pluginPath);
        PluginDescriptor pluginDescriptor = descriptorFinder.find(pluginPath);
        validatePluginDescriptor(pluginDescriptor);

        // Verifique se não há plug-ins carregados com a id recuperada
        pluginId = pluginDescriptor.getPluginId();
        if (plugins.containsKey(pluginId)) {
            PluginWrapper loadedPlugin = getPlugin(pluginId);
            throw new PluginRuntimeException(StringUtils.format("Já existe um plugin carregado (%s) "
                            + "com o mesmo id (%s) que o archbasePlugin no caminho '%s'. Carregamento simultâneo "
                            + "de plug-ins com o mesmo PluginId não é compatível no momento. \n"
                            + "Como solução alternativa, você pode incluir PluginVersion e PluginProvider "
                            + "no PluginId.",
                    loadedPlugin, pluginId, pluginPath));
        }

        log.debug("Descritor encontrado {}", pluginDescriptor);
        String pluginClassName = pluginDescriptor.getPluginClass();
        log.debug("Classe '{}' para archbasePlugin '{}'", pluginClassName, pluginPath);

        // carregar archbasePlugin
        log.debug("Carregando archbasePlugin '{}'", pluginPath);
        ClassLoader pluginClassLoader = getPluginLoader().loadPlugin(pluginPath, pluginDescriptor);
        log.debug("ArchbasePlugin carregado '{}' com carregador de classe '{}'", pluginPath, pluginClassLoader);

        // crie o wrapper do archbasePlugin
        log.debug("Criando wrapper para archbasePlugin '{}'", pluginPath);
        PluginWrapper pluginWrapper = new PluginWrapper(this, pluginDescriptor, pluginPath, pluginClassLoader);
        pluginWrapper.setPluginFactory(getPluginFactory());

        // teste para archbasePlugin desativado
        if (isPluginDisabled(pluginDescriptor.getPluginId())) {
            log.info("ArchbasePlugin '{}' está desativado", pluginPath);
            pluginWrapper.setPluginState(PluginState.DISABLED);
        }

        // valide o archbasePlugin
        if (!isPluginValid(pluginWrapper)) {
            log.warn("O plug-in '{}' é inválido e será desativado", pluginPath);
            pluginWrapper.setPluginState(PluginState.DISABLED);
        }

        log.debug("Wrapper '{}' criado para o archbasePlugin '{}'", pluginWrapper, pluginPath);

        pluginId = pluginDescriptor.getPluginId();

        // adicionar archbasePlugin à lista com plugins
        plugins.put(pluginId, pluginWrapper);
        getUnresolvedPlugins().add(pluginWrapper);

        // adicionar carregador de classe de archbasePlugin à lista com carregadores de classe
        getPluginClassLoaders().put(pluginId, pluginClassLoader);

        return pluginWrapper;
    }

    /**
     * Testes para plug-ins já carregados em determinado caminho.
     *
     * @param pluginPath o caminho a ser investigado
     * @return id do archbasePlugin ou nulo se não carregado
     */
    protected String idForPath(Path pluginPath) {
        for (PluginWrapper plugin : plugins.values()) {
            if (plugin.getPluginPath().equals(pluginPath)) {
                return plugin.getPluginId();
            }
        }

        return null;
    }

    /**
     * Substitua isso para alterar os critérios de validação.
     *
     * @param descriptor o descritor de archbasePlugin para validar
     * @throws PluginRuntimeException se a validação falhar
     */
    protected void validatePluginDescriptor(PluginDescriptor descriptor) {
        if (StringUtils.isNullOrEmpty(descriptor.getPluginId())) {
            throw new PluginRuntimeException("O campo 'id' não pode estar vazio");
        }

        if (descriptor.getVersion() == null) {
            throw new PluginRuntimeException("O campo 'versão' não pode estar vazio");
        }
    }

    /**
     * @return true se versões exatas em requer forem permitidas
     */
    public boolean isExactVersionAllowed() {
        return exactVersionAllowed;
    }

    /**
     * Defina como verdadeiro para permitir que a expressão de requer seja exatamente x.y.z.
     * O padrão é falso, o que significa que usar uma versão exata x.y.z
     * implicitamente significa o mesmo que> = x.y.z
     *
     * @param exactVersionAllowed definido como verdadeiro ou falso
     */
    public void setExactVersionAllowed(boolean exactVersionAllowed) {
        this.exactVersionAllowed = exactVersionAllowed;
    }

    @Override
    public VersionManager getVersionManager() {
        return versionManager;
    }

    /**
     * O rótulo do archbasePlugin é usado no registro e é uma string no formato {@code pluginId @ pluginVersion}.
     */
    protected String getPluginLabel(PluginDescriptor pluginDescriptor) {
        return pluginDescriptor.getPluginId() + "@" + pluginDescriptor.getVersion();
    }

    @SuppressWarnings("unchecked")
    protected <T> List<Class<? extends T>> getExtensionClasses(List<ExtensionWrapper<T>> extensionsWrapper) {
        List<Class<? extends T>> extensionClasses = new ArrayList<>(extensionsWrapper.size());
        for (ExtensionWrapper<T> extensionWrapper : extensionsWrapper) {
            Class<T> c = (Class<T>) extensionWrapper.getDescriptor().extensionClass;
            extensionClasses.add(c);
        }

        return extensionClasses;
    }

    protected <T> List<T> getExtensions(List<ExtensionWrapper<T>> extensionsWrapper) {
        List<T> extensions = new ArrayList<>(extensionsWrapper.size());
        for (ExtensionWrapper<T> extensionWrapper : extensionsWrapper) {
            try {
                extensions.add(extensionWrapper.getExtension());
            } catch (PluginRuntimeException e) {
                log.error("Não é possível recuperar a extensão", e);
            }
        }

        return extensions;
    }


    protected PluginRepository getPluginRepository() {
        return pluginRepository;
    }

}
