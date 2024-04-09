package br.com.archbase.plugin.manager;


public interface PluginStatusProvider {

    /**
     * Verifica se o archbasePlugin está desabilitado ou não
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @return se o archbasePlugin está desabilitado ou não
     */
    boolean isPluginDisabled(String pluginId);

    /**
     * Desabilita o carregamento de um archbasePlugin.
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @throws PluginRuntimeException se algo der errado
     */
    void disablePlugin(String pluginId);

    /**
     * Ativa um archbasePlugin que foi desativado anteriormente.
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @throws PluginRuntimeException se algo der errado
     */
    void enablePlugin(String pluginId);

}
