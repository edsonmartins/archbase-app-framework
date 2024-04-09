package br.com.archbase.plugin.manager;

import java.nio.file.Path;

/**
 * Encontre um descritor de archbasePlugin para um caminho de archbasePlugin.
 * Você pode encontrar o descritor do plug-in no arquivo de manifesto {@link ManifestPluginDescriptorFinder},
 * arquivo de propriedades {@link PropertiesPluginDescriptorFinder}, arquivo xml,
 * serviços java (com {@link java.util.ServiceLoader}), etc.
 */
public interface PluginDescriptorFinder {

    /**
     * Retorna verdadeiro se este localizador for aplicável ao {@link Path} fornecido.
     */
    boolean isApplicable(Path pluginPath);

    PluginDescriptor find(Path pluginPath);

}
