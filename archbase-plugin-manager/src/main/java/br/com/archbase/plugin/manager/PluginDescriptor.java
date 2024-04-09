package br.com.archbase.plugin.manager;

import java.util.List;

/**
 * Um descritor de plug-in contém informações sobre um plug-in.
 */
public interface PluginDescriptor {

    String getPluginId();

    String getPluginDescription();

    String getPluginClass();

    String getVersion();

    String getRequires();

    String getProvider();

    String getLicense();

    List<PluginDependency> getDependencies();

}
