package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Carregue todas as informações necessárias para um archbasePlugin.
 * Isso significa adicionar ao {@link ClassLoader} do archbasePlugin todos os arquivos jar e
 * todos os arquivos de classe especificados em {@link PluginClasspath}.
 */
public class BaseArchbasePluginLoader implements ArchbasePluginLoader {

    protected ArchbasePluginManager archbasePluginManager;
    protected PluginClasspath pluginClasspath;

    public BaseArchbasePluginLoader(ArchbasePluginManager archbasePluginManager, PluginClasspath pluginClasspath) {
        this.archbasePluginManager = archbasePluginManager;
        this.pluginClasspath = pluginClasspath;
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        return Files.exists(pluginPath);
    }

    @Override
    public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
        PluginClassLoader pluginClassLoader = createPluginClassLoader(pluginPath, pluginDescriptor);

        loadClasses(pluginPath, pluginClassLoader);
        loadJars(pluginPath, pluginClassLoader);

        return pluginClassLoader;
    }

    @SuppressWarnings("java:S1172")
    protected PluginClassLoader createPluginClassLoader(Path pluginPath, PluginDescriptor pluginDescriptor) {
        return new PluginClassLoader(archbasePluginManager, pluginDescriptor, getClass().getClassLoader());
    }

    /**
     * Adicione todos os arquivos {@code *.class} de {@link PluginClasspath #getClassesDirectories()}
     * para o {@link ClassLoader} do plug-in.
     */
    protected void loadClasses(Path pluginPath, PluginClassLoader pluginClassLoader) {
        for (String directory : pluginClasspath.getClassesDirectories()) {
            File file = pluginPath.resolve(directory).toFile();

            if (file.exists() && file.isDirectory()) {
                pluginClassLoader.addFile(file);
            }
        }

        Collection<File> directories = org.apache.commons.io.FileUtils.listFilesAndDirs(pluginPath.toFile(), new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return false;
            }

            @Override
            public boolean accept(File dir, String name) {
                return false;
            }
        }, new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return false;
            }

            @Override
            public boolean accept(File dir, String name) {
                return true;
            }
        });

        for (File directory : directories) {
            pluginClassLoader.addFile(directory);
        }
    }

    /**
     * Adicione todos os arquivos {@code * .jar} de {@link PluginClasspath#getJarsDirectories()}
     * para o {@link ClassLoader} do plug-in.
     */
    protected void loadJars(Path pluginPath, PluginClassLoader pluginClassLoader) {
        for (String jarsDirectory : pluginClasspath.getJarsDirectories()) {
            Path file = pluginPath.resolve(jarsDirectory);
            List<File> jars = FileUtils.getJars(file);
            for (File jar : jars) {
                pluginClassLoader.addFile(jar);
            }
        }
    }

}
