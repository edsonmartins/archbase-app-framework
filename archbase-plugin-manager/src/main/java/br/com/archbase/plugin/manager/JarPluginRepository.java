package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.JarFileFilter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


public class JarPluginRepository extends BasePluginRepository {

    public JarPluginRepository(Path... pluginsRoots) {
        this(Arrays.asList(pluginsRoots));
    }

    public JarPluginRepository(List<Path> pluginsRoots) {
        super(pluginsRoots, new JarFileFilter());
    }

}
