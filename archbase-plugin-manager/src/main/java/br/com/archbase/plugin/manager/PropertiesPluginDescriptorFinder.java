package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;
import br.com.archbase.plugin.manager.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Encontre um descritor de archbasePlugin em um arquivo de propriedades (no repositório de archbasePlugin).
 */
public class PropertiesPluginDescriptorFinder implements PluginDescriptorFinder {

    public static final String DEFAULT_PROPERTIES_FILE_NAME = "plugin.properties";
    public static final String PLUGIN_ID = "plugin.id";
    public static final String PLUGIN_DESCRIPTION = "plugin.description";
    public static final String PLUGIN_CLASS = "plugin.class";
    public static final String PLUGIN_VERSION = "plugin.version";
    public static final String PLUGIN_PROVIDER = "plugin.provider";
    public static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";
    public static final String PLUGIN_REQUIRES = "plugin.requires";
    public static final String PLUGIN_LICENSE = "plugin.license";
    private static final Logger log = LoggerFactory.getLogger(PropertiesPluginDescriptorFinder.class);
    protected String propertiesFileName;

    public PropertiesPluginDescriptorFinder() {
        this(DEFAULT_PROPERTIES_FILE_NAME);
    }

    public PropertiesPluginDescriptorFinder(String propertiesFileName) {
        this.propertiesFileName = propertiesFileName;
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        return Files.exists(pluginPath) && (Files.isDirectory(pluginPath) || FileUtils.isJarFile(pluginPath));
    }

    @Override
    public PluginDescriptor find(Path pluginPath) {
        Properties properties = readProperties(pluginPath);

        return createPluginDescriptor(properties);
    }

    protected Properties readProperties(Path pluginPath) {
        Path propertiesPath = getPropertiesPath(pluginPath, propertiesFileName);
        if (propertiesPath == null) {
            throw new PluginRuntimeException("Não é possível encontrar o caminho das propriedades");
        }

        Properties properties = new Properties();
        try {
            log.debug("Descritor de archbasePlugin de pesquisa em '{}'", propertiesPath);
            if (Files.notExists(propertiesPath)) {
                throw new PluginRuntimeException("Não é possível encontrar o caminho " + propertiesPath);
            }

            try (InputStream input = Files.newInputStream(propertiesPath)) {
                properties.load(input);
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        } finally {
            FileUtils.closePath(propertiesPath);
        }

        return properties;
    }

    protected Path getPropertiesPath(Path pluginPath, String propertiesFileName) {
        if (Files.isDirectory(pluginPath)) {
            return pluginPath.resolve(Paths.get(propertiesFileName));
        } else {
            // é um arquivo jar
            try {
                return FileUtils.getPath(pluginPath, propertiesFileName);
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        }
    }

    protected PluginDescriptor createPluginDescriptor(Properties properties) {
        DefaultArchbasePluginDescriptor pluginDescriptor = createPluginDescriptorInstance();

        // TODO validar !!!
        String id = properties.getProperty(PLUGIN_ID);
        pluginDescriptor.setPluginId(id);

        String description = properties.getProperty(PLUGIN_DESCRIPTION);
        if (StringUtils.isNullOrEmpty(description)) {
            pluginDescriptor.setPluginDescription("");
        } else {
            pluginDescriptor.setPluginDescription(description);
        }

        String clazz = properties.getProperty(PLUGIN_CLASS);
        if (StringUtils.isNotNullOrEmpty(clazz)) {
            pluginDescriptor.setPluginClass(clazz);
        }

        String version = properties.getProperty(PLUGIN_VERSION);
        if (StringUtils.isNotNullOrEmpty(version)) {
            pluginDescriptor.setPluginVersion(version);
        }

        String provider = properties.getProperty(PLUGIN_PROVIDER);
        pluginDescriptor.setProvider(provider);

        String dependencies = properties.getProperty(PLUGIN_DEPENDENCIES);
        pluginDescriptor.setDependencies(dependencies);

        String requires = properties.getProperty(PLUGIN_REQUIRES);
        if (StringUtils.isNotNullOrEmpty(requires)) {
            pluginDescriptor.setRequires(requires);
        }

        pluginDescriptor.setLicense(properties.getProperty(PLUGIN_LICENSE));

        return pluginDescriptor;
    }

    protected DefaultArchbasePluginDescriptor createPluginDescriptorInstance() {
        return new DefaultArchbasePluginDescriptor();
    }

}
