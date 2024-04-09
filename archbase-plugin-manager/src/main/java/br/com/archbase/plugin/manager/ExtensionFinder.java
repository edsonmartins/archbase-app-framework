package br.com.archbase.plugin.manager;

import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public interface ExtensionFinder {

    /**
     * Recupera uma lista com todas as extensões encontradas para um ponto de extensão.
     */
    <T> List<ExtensionWrapper<T>> find(Class<T> type);

    /**
     * Recupera uma lista com todas as extensões encontradas para um ponto de extensão e um archbasePlugin.
     */
    <T> List<ExtensionWrapper<T>> find(Class<T> type, String pluginId);

    /**
     * Recupera uma lista com todas as extensões encontradas para um archbasePlugin
     */
    List<ExtensionWrapper> find(String pluginId);

    /**
     * Recupera uma lista com todos os nomes de classe de extensão encontrados para um plug-in.
     */
    Set<String> findClassNames(String pluginId);

}
