package br.com.archbase.plugin.manager;

import java.nio.file.Path;
import java.util.List;

/**
 * Diretório que contém plug-ins. Um plug-in pode ser um arquivo {@code directory}, @code zip} ou {@code jar}.
 */
public interface PluginRepository {

    /**
     * Liste todos os caminhos do archbasePlugin.
     *
     * @return uma lista com caminhos
     */
    List<Path> getPluginPaths();

    /**
     * Remove um archbasePlugin do repositório.
     *
     * @param pluginPath o caminho do archbasePlugin
     * @return true se excluído
     * @throws PluginRuntimeException se algo der errado
     */
    boolean deletePluginPath(Path pluginPath);


    /**
     * Liste todos os caminhos de plug-ins.
     *
     * @return uma lista com caminhos
     */
    List<Path> getPluginsPaths();

}
