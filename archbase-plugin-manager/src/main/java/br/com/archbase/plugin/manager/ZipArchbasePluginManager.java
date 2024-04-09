package br.com.archbase.plugin.manager;

/**
 * É um {@link ArchbasePluginManager} que carrega cada archbasePlugin de um arquivo {@code zip}.
 * A estrutura do arquivo zip é:
 * <ul>
 * <li> {@code lib} diretório que contém todas as dependências (como arquivos jar); é opcional (sem dependências)
 * <li> {@code classes} diretório que contém todas as classes do archbasePlugin
 * </ul>
 */
public class ZipArchbasePluginManager extends DefaultArchbasePluginManager {

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new PropertiesPluginDescriptorFinder();
    }

    @Override
    protected ArchbasePluginLoader createPluginLoader() {
        return new CompoundArchbasePluginLoader()
                .add(new DevelopmentArchbasePluginLoader(this), this::isDevelopment)
                .add(new DefaultArchbasePluginLoader(this), this::isNotDevelopment);
    }

    @Override
    protected PluginRepository createPluginRepository() {
        return new CompoundPluginRepository()
                .add(new DevelopmentPluginRepository(getPluginsRoots()), this::isDevelopment)
                .add(new DefaultArchbasePluginRepository(getPluginsRoots()), this::isNotDevelopment);
    }

}
