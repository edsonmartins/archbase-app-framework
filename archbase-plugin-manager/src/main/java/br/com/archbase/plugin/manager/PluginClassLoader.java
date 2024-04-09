package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Uma instância desta classe deve ser criada pelo gerenciador de plug-ins para cada plug-in disponível.
 * Por padrão, este carregador de classes é um Pai e Último ClassLoader - ele carrega as classes dos jars do archbasePlugin
 * antes de delegar ao carregador de classe pai.
 * Use {@link #classLoadingStrategy} para alterar a estratégia de carregamento.
 */
public class PluginClassLoader extends URLClassLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginClassLoader.class);

    private static final String JAVA_PACKAGE_PREFIX = "java.";
    private static final String PLUGIN_PACKAGE_PREFIX = "br.com.archbase.";

    private ArchbasePluginManager archbasePluginManager;
    private PluginDescriptor pluginDescriptor;
    private ClassLoadingStrategy classLoadingStrategy;

    public PluginClassLoader(ArchbasePluginManager archbasePluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent) {
        this(archbasePluginManager, pluginDescriptor, parent, ClassLoadingStrategy.PDA);
    }

    /**
     * carregamento de classe de acordo com {@code classLoadingStrategy}
     */
    public PluginClassLoader(ArchbasePluginManager archbasePluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent, ClassLoadingStrategy classLoadingStrategy) {
        super(new URL[0], parent);

        this.archbasePluginManager = archbasePluginManager;
        this.pluginDescriptor = pluginDescriptor;
        this.classLoadingStrategy = classLoadingStrategy;
    }

    /**
     * If {@code parentFirst} is {@code true}, indicates that the parent {@link ClassLoader} should be consulted
     * before trying to load the a class through this loader.
     */
    public PluginClassLoader(ArchbasePluginManager archbasePluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent, boolean parentFirst) {
        this(archbasePluginManager, pluginDescriptor, parent, parentFirst ? ClassLoadingStrategy.APD : ClassLoadingStrategy.PDA);
    }

    @Override
    public void addURL(URL url) {
        log.debug("Adicionando '{}'", url);
        super.addURL(url);
    }

    public void addFile(File file) {
        try {
            addURL(file.getCanonicalFile().toURI().toURL());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Por padrão, ele usa um modelo de delegação primeiro filho em vez do pai padrão primeiro.
     * Se a classe solicitada não puder ser encontrada neste carregador de classes, o carregador de classes pai será consultado
     * por meio do mecanismo padrão {@link ClassLoader # loadClass (String)}.
     * Use {@link #classLoadingStrategy} para alterar a estratégia de carregamento.
     */
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(className)) {
            // primeiro verifique se é uma classe de sistema, delegue ao carregador do sistema
            if (className.startsWith(JAVA_PACKAGE_PREFIX)) {
                return findSystemClass(className);
            }

            // se a classe faz parte do mecanismo de plug-in, use o carregador de classe pai
            if (className.startsWith(PLUGIN_PACKAGE_PREFIX) && !className.startsWith("br.com.archbase.plugin.demo")) {
                try {
                    Class<?> loadClass = getParent().loadClass(className);
                    if (loadClass != null) {
                        return loadClass;
                    }
                } catch (ClassNotFoundException ignored) {
                    //
                }
            }

            log.trace("Pedido recebido para carregar a classe'{}'", className);

            // segundo verifique se já foi carregado
            Class<?> loadedClass = findLoadedClass(className);
            if (loadedClass != null) {
                log.trace("Classe carregada encontrada'{}'", className);
                return loadedClass;
            }

            for (ClassLoadingStrategy.Source classLoadingSource : classLoadingStrategy.getSources()) {
                Class<?> c = null;
                try {
                    switch (classLoadingSource) {
                        case PLUGIN:
                            c = findClass(className);
                            break;
                        case DEPENDENCIES:
                            c = loadClassFromDependencies(className);
                            break;
                        default:
                            c = super.loadClass(className);
                            break;
                    }
                } catch (ClassNotFoundException ignored) {
                    //
                }

                if (c != null) {
                    log.trace("Classe '{}' encontrada no {} caminho de classe", className, classLoadingSource);
                    return c;
                } else {
                    log.trace("Não foi possível encontrar a classe '{}' no {} classpath", className, classLoadingSource);
                }
            }

            throw new ClassNotFoundException(className);
        }
    }

    /**
     * Carregue o recurso nomeado deste archbasePlugin.
     * Por padrão, esta implementação verifica primeiro o classpath do archbasePlugin e depois delega ao pai.
     * Use {@link #classLoadingStrategy} para alterar a estratégia de carregamento.
     *
     * @param name o nome do recurso.
     * @return o URL para o recurso, {@code null} se o recurso não foi encontrado.
     */
    @Override
    public URL getResource(String name) {
        log.trace("Pedido recebido para carregar o recurso '{}'", name);
        for (ClassLoadingStrategy.Source classLoadingSource : classLoadingStrategy.getSources()) {
            URL url = null;
            switch (classLoadingSource) {
                case PLUGIN:
                    url = findResource(name);
                    break;
                case DEPENDENCIES:
                    url = findResourceFromDependencies(name);
                    break;
                default:
                    url = super.getResource(name);
                    break;
            }

            if (url != null) {
                log.trace("Recurso '{}' encontrado em {} classpath", name, classLoadingSource);
                return url;
            } else {
                log.trace("Não foi possível encontrar recurso '{}' in {}", name, classLoadingSource);
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>();

        log.trace("Pedido recebido para carregar recursos '{}'", name);
        for (ClassLoadingStrategy.Source classLoadingSource : classLoadingStrategy.getSources()) {
            switch (classLoadingSource) {
                case PLUGIN:
                    resources.addAll(Collections.list(findResources(name)));
                    break;
                case DEPENDENCIES:
                    resources.addAll(findResourcesFromDependencies(name));
                    break;
                default:
                    if (getParent() != null) {
                        resources.addAll(Collections.list(getParent().getResources(name)));
                    }
                    break;
            }
        }

        return Collections.enumeration(resources);
    }

    protected Class<?> loadClassFromDependencies(String className) {
        log.trace("Pesquisar nas dependências da classe '{}'", className);
        List<PluginDependency> dependencies = pluginDescriptor.getDependencies();
        for (PluginDependency dependency : dependencies) {
            ClassLoader classLoader = archbasePluginManager.getPluginClassLoader(dependency.getPluginId());

            // Se a dependência estiver marcada como opcional, seu carregador de classes pode não estar disponível.
            if (classLoader != null) {
                try {
                    return classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    // tente a próxima dependência
                }
            }
        }

        return null;
    }

    protected URL findResourceFromDependencies(String name) {
        log.trace("Pesquisar nas dependências do recurso '{}'", name);
        List<PluginDependency> dependencies = pluginDescriptor.getDependencies();
        for (PluginDependency dependency : dependencies) {
            PluginClassLoader classLoader = (PluginClassLoader) archbasePluginManager.getPluginClassLoader(dependency.getPluginId());

            // Se a dependência estiver marcada como opcional, seu carregador de classes pode não estar disponível.
            if (classLoader != null) {
                URL url = classLoader.findResource(name);
                if (Objects.nonNull(url)) {
                    return url;
                }
            }
        }

        return null;
    }

    protected Collection<URL> findResourcesFromDependencies(String name) throws IOException {
        log.trace("Pesquisar nas dependências de recursos '{}'", name);
        List<URL> results = new ArrayList<>();
        List<PluginDependency> dependencies = pluginDescriptor.getDependencies();
        for (PluginDependency dependency : dependencies) {
            PluginClassLoader classLoader = (PluginClassLoader) archbasePluginManager.getPluginClassLoader(dependency.getPluginId());

            // If the dependency is marked as optional, its class loader might not be available.
            if (classLoader != null) {
                results.addAll(Collections.list(classLoader.findResources(name)));
            }
        }

        return results;
    }

}
