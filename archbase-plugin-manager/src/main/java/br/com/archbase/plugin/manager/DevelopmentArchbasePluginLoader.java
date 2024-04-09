package br.com.archbase.plugin.manager;

/**
 * Carregue todas as informações necessárias para um plug-in de {@link DevelopmentPluginClasspath}.
 */
public class DevelopmentArchbasePluginLoader extends BaseArchbasePluginLoader {

    public DevelopmentArchbasePluginLoader(ArchbasePluginManager archbasePluginManager) {
        super(archbasePluginManager, new DevelopmentPluginClasspath());
    }

}
