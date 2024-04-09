package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Implementação padrão da interface {@link ArchbasePluginManager}.
 * Em essência, é um {@link ZipArchbasePluginManager} mais um {@link JarArchbasePluginManager}.
 * Portanto, ele pode carregar plug-ins do jar e do zip, simultaneamente.
 *
 * <p> Esta classe não é thread-safe.
 */
public class DefaultArchbasePluginManager extends AbstractArchbasePluginManager {

    public static final String PLUGINS_DIR_CONFIG_PROPERTY_NAME = "archbase.pluginsConfigDir";
    private static final Logger log = LoggerFactory.getLogger(DefaultArchbasePluginManager.class);

    public DefaultArchbasePluginManager() {
        super();
    }

    public DefaultArchbasePluginManager(Path... pluginsRoots) {
        super(pluginsRoots);
    }

    public DefaultArchbasePluginManager(List<Path> pluginsRoots) {
        super(pluginsRoots);
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new CompoundPluginDescriptorFinder()
                .add(new PropertiesPluginDescriptorFinder())
                .add(new ManifestPluginDescriptorFinder());
    }

    @Override
    protected ExtensionFinder createExtensionFinder() {
        DefaultArchbaseExtensionFinder extensionFinder = new DefaultArchbaseExtensionFinder(this);
        addPluginStateListener(extensionFinder);

        return extensionFinder;
    }

    @Override
    protected ArchbasePluginFactory createPluginFactory() {
        return new DefaultArchbasePluginFactory();
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new DefaultArchbaseExtensionFactory();
    }

    @Override
    protected PluginStatusProvider createPluginStatusProvider() {
        String configDir = System.getProperty(PLUGINS_DIR_CONFIG_PROPERTY_NAME);
        Path configPath = configDir != null
                ? Paths.get(configDir)
                : getPluginsRoots().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nenhum plug-inRoot configurado"));

        return new DefaultArchbasePluginStatusProvider(configPath);
    }

    @Override
    protected PluginRepository createPluginRepository() {
        return new CompoundPluginRepository()
                .add(new DevelopmentPluginRepository(getPluginsRoots()), this::isDevelopment)
                .add(new JarPluginRepository(getPluginsRoots()), this::isNotDevelopment)
                .add(new DefaultArchbasePluginRepository(getPluginsRoots()), this::isNotDevelopment);
    }

    @Override
    protected ArchbasePluginLoader createPluginLoader() {
        return new CompoundArchbasePluginLoader()
                .add(new DevelopmentArchbasePluginLoader(this), this::isDevelopment)
                .add(new JarArchbasePluginLoader(this), this::isNotDevelopment)
                .add(new DefaultArchbasePluginLoader(this), this::isNotDevelopment);
    }

    @Override
    protected VersionManager createVersionManager() {
        return new DefaultArchbaseVersionManager();
    }

    @Override
    protected void initialize() {
        super.initialize();

        if (isDevelopment()) {
            addPluginStateListener(new LoggingPluginStateListener());
        }

        log.info("Archbase versão {} em modo '{}' ", getVersion(), getRuntimeMode());
    }

    /**
     * Carregue um archbasePlugin do disco. Se o caminho for um arquivo zip, descompacte primeiro.
     *
     * @param pluginPath localização do archbasePlugin no disco
     * @return PluginWrapper para o archbasePlugin carregado ou nulo se não estiver carregado
     * @throws PluginRuntimeException se houver problemas durante o carregamento
     */
    @Override
    protected PluginWrapper loadPluginFromPath(Path pluginPath) {
        // Primeiro descompacte quaisquer arquivos ZIP
        try {
            pluginPath = FileUtils.expandIfZip(pluginPath);
        } catch (Exception e) {
            log.warn("Falha ao descompactar {}", pluginPath, e);
            return null;
        }

        return super.loadPluginFromPath(pluginPath);
    }

}
