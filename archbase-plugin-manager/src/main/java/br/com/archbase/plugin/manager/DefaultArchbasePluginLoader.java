package br.com.archbase.plugin.manager;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carregue todas as informações necessárias para um archbasePlugin de {@link DefaultArchbasePluginClasspath}.
 */
public class DefaultArchbasePluginLoader extends BaseArchbasePluginLoader {

    public DefaultArchbasePluginLoader(ArchbasePluginManager archbasePluginManager) {
        super(archbasePluginManager, new DefaultArchbasePluginClasspath());
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        return super.isApplicable(pluginPath) && Files.isDirectory(pluginPath);
    }

}
