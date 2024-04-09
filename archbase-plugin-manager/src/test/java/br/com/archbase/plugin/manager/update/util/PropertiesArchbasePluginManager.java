package br.com.archbase.plugin.manager.update.util;


import br.com.archbase.plugin.manager.DefaultArchbasePluginManager;
import br.com.archbase.plugin.manager.PluginDescriptorFinder;
import br.com.archbase.plugin.manager.PropertiesPluginDescriptorFinder;

import java.nio.file.Path;

/**
 * Manager usando propriedades em vez de manifesto, para teste.
 */
public class PropertiesArchbasePluginManager extends DefaultArchbasePluginManager {

    public PropertiesArchbasePluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new PropertiesPluginDescriptorFinder("my.properties");
    }

    @Override
    protected PluginDescriptorFinder getPluginDescriptorFinder() {
        return new PropertiesPluginDescriptorFinder("my.properties");
    }

}
