package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;


public class JarArchbasePluginLoader implements ArchbasePluginLoader {

    protected ArchbasePluginManager archbasePluginManager;

    public JarArchbasePluginLoader(ArchbasePluginManager archbasePluginManager) {
        this.archbasePluginManager = archbasePluginManager;
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        return Files.exists(pluginPath) && FileUtils.isJarFile(pluginPath);
    }

    @Override
    public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
        PluginClassLoader pluginClassLoader = new PluginClassLoader(archbasePluginManager, pluginDescriptor, getClass().getClassLoader());
        pluginClassLoader.addFile(pluginPath.toFile());

        return pluginClassLoader;
    }

}
