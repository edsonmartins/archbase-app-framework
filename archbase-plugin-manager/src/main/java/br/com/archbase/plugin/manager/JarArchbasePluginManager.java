package br.com.archbase.plugin.manager;

import java.nio.file.Path;

/**
 * É um {@link ArchbasePluginManager} que carrega cada archbasePlugin de um arquivo {@code jar}.
 * Na verdade, um archbasePlugin é um jar gordo, um jar que contém classes de todas as bibliotecas,
 * da qual depende o seu projeto e, claro, as aulas do projeto atual.
 */
public class JarArchbasePluginManager extends DefaultArchbasePluginManager {

    public JarArchbasePluginManager() {
        super();
    }

    public JarArchbasePluginManager(Path... pluginsRoots) {
        super(pluginsRoots);
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new ManifestPluginDescriptorFinder();
    }

    @Override
    protected ArchbasePluginLoader createPluginLoader() {
        return new CompoundArchbasePluginLoader()
                .add(new DevelopmentArchbasePluginLoader(this), this::isDevelopment)
                .add(new JarArchbasePluginLoader(this), this::isNotDevelopment);
    }

    @Override
    protected PluginRepository createPluginRepository() {
        return new CompoundPluginRepository()
                .add(new DevelopmentPluginRepository(getPluginsRoots()), this::isDevelopment)
                .add(new JarPluginRepository(getPluginsRoots()), this::isNotDevelopment);
    }

}
