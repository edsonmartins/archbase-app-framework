package br.com.archbase.plugin.manager;

import java.nio.file.Path;

/**
 * Carregue todas as informações (classes) necessárias para um archbasePlugin.
 */
public interface ArchbasePluginLoader {

    /**
     * Retorna verdadeiro se este carregador for aplicável ao {@link Path} fornecido.
     *
     * @param pluginPath
     * @return
     */
    boolean isApplicable(Path pluginPath);

    ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor);

}
