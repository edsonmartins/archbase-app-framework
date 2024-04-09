package br.com.archbase.plugin.manager;

/**
 * Cria uma instância de archbasePlugin.
 */
public interface ArchbasePluginFactory {

    ArchbasePlugin create(PluginWrapper pluginWrapper);

}
