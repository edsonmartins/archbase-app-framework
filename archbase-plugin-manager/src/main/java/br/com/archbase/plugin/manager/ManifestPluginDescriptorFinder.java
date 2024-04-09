package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;
import br.com.archbase.plugin.manager.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Leia o descritor do archbasePlugin do arquivo de manifesto.
 */
public class ManifestPluginDescriptorFinder implements PluginDescriptorFinder {

    public static final String PLUGIN_ID = "Plugin-Id";
    public static final String PLUGIN_DESCRIPTION = "Plugin-Description";
    public static final String PLUGIN_CLASS = "Plugin-Class";
    public static final String PLUGIN_VERSION = "Plugin-Version";
    public static final String PLUGIN_PROVIDER = "Plugin-Provider";
    public static final String PLUGIN_DEPENDENCIES = "Plugin-Dependencies";
    public static final String PLUGIN_REQUIRES = "Plugin-Requires";
    public static final String PLUGIN_LICENSE = "Plugin-License";
    private static final Logger log = LoggerFactory.getLogger(ManifestPluginDescriptorFinder.class);

    @Override
    public boolean isApplicable(Path pluginPath) {
        return Files.exists(pluginPath) && (Files.isDirectory(pluginPath) || FileUtils.isJarFile(pluginPath));
    }

    @Override
    public PluginDescriptor find(Path pluginPath) {
        Manifest manifest = readManifest(pluginPath);

        return createPluginDescriptor(manifest);
    }

    protected Manifest readManifest(Path pluginPath) {
        if (FileUtils.isJarFile(pluginPath)) {
            try (JarFile jar = new JarFile(pluginPath.toFile())) {
                Manifest manifest = jar.getManifest();
                if (manifest != null) {
                    return manifest;
                }
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        }

        Path manifestPath = getManifestPath(pluginPath);
        if (manifestPath == null) {
            throw new PluginRuntimeException("Não é possível encontrar o caminho do manifesto");
        }

        log.debug("Descritor de archbasePlugin de pesquisa em '{}'", manifestPath);
        if (Files.notExists(manifestPath)) {
            throw new PluginRuntimeException("Não é possível encontrar o caminho " + manifestPath);
        }

        try (InputStream input = Files.newInputStream(manifestPath)) {
            return new Manifest(input);
        } catch (IOException e) {
            throw new PluginRuntimeException(e);
        }
    }

    protected Path getManifestPath(Path pluginPath) {
        if (Files.isDirectory(pluginPath)) {
            // legado (o caminho é algo como "classes/META-INF/MANIFEST.MF")
            return FileUtils.findFile(pluginPath, "MANIFEST.MF");
        }

        return null;
    }

    protected PluginDescriptor createPluginDescriptor(Manifest manifest) {
        DefaultArchbasePluginDescriptor pluginDescriptor = createPluginDescriptorInstance();

        // TODO validar !!!
        Attributes attributes = manifest.getMainAttributes();
        String id = attributes.getValue(PLUGIN_ID);
        pluginDescriptor.setPluginId(id);

        String description = attributes.getValue(PLUGIN_DESCRIPTION);
        if (StringUtils.isNullOrEmpty(description)) {
            pluginDescriptor.setPluginDescription("");
        } else {
            pluginDescriptor.setPluginDescription(description);
        }

        String clazz = attributes.getValue(PLUGIN_CLASS);
        if (StringUtils.isNotNullOrEmpty(clazz)) {
            pluginDescriptor.setPluginClass(clazz);
        }

        String version = attributes.getValue(PLUGIN_VERSION);
        if (StringUtils.isNotNullOrEmpty(version)) {
            pluginDescriptor.setPluginVersion(version);
        }

        String provider = attributes.getValue(PLUGIN_PROVIDER);
        pluginDescriptor.setProvider(provider);
        String dependencies = attributes.getValue(PLUGIN_DEPENDENCIES);
        pluginDescriptor.setDependencies(dependencies);

        String requires = attributes.getValue(PLUGIN_REQUIRES);
        if (StringUtils.isNotNullOrEmpty(requires)) {
            pluginDescriptor.setRequires(requires);
        }

        pluginDescriptor.setLicense(attributes.getValue(PLUGIN_LICENSE));

        return pluginDescriptor;
    }

    protected DefaultArchbasePluginDescriptor createPluginDescriptorInstance() {
        return new DefaultArchbasePluginDescriptor();
    }

}
