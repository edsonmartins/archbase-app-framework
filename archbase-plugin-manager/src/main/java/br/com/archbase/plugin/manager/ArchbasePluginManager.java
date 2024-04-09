package br.com.archbase.plugin.manager;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Fornece a funcionalidade de gerenciamento de plug-ins, como carga,
 * iniciar e parar os plug-ins.
 */
public interface ArchbasePluginManager {

    /**
     * Recupera todos os plug-ins.
     */
    List<PluginWrapper> getPlugins();

    /**
     * Recupera todos os plug-ins com este estado.
     */
    List<PluginWrapper> getPlugins(PluginState pluginState);

    /**
     * Recupera todos os plug-ins resolvidos (com dependência resolvida).
     */
    List<PluginWrapper> getResolvedPlugins();

    /**
     * Recupera todos os plug-ins não resolvidos (com dependência não resolvida).
     */
    List<PluginWrapper> getUnresolvedPlugins();

    /**
     * Recupera todos os plug-ins iniciados.
     */
    List<PluginWrapper> getStartedPlugins();

    /**
     * Recupera o archbasePlugin com este id, ou null se o archbasePlugin não existe.
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @return Um objeto PluginWrapper para este plug-in ou null se ele não existir.
     */
    PluginWrapper getPlugin(String pluginId);

    /**
     * Carregar plugins.
     */
    void loadPlugins();

    /**
     * Carregue um archbasePlugin.
     *
     * @param pluginPath a localização do archbasePlugin
     * @return o pluginId do archbasePlugin instalado conforme especificado em seus {@linkplain PluginDescriptor metadata}
     * @throws PluginRuntimeException se algo der errado
     */
    String loadPlugin(Path pluginPath);

    /**
     * Inicie todos os plug-ins ativos.
     */
    void startPlugins();

    /**
     * Inicie o archbasePlugin especificado e suas dependências.
     *
     * @return o estado do archbasePlugin
     * @throws PluginRuntimeException se algo der errado
     */
    PluginState startPlugin(String pluginId);

    /**
     * Pare todos os plug-ins ativos.
     */
    void stopPlugins();

    /**
     * Pare o archbasePlugin especificado e suas dependências.
     *
     * @return o estado do archbasePlugin
     * @throws PluginRuntimeException se algo der errado
     */
    PluginState stopPlugin(String pluginId);

    /**
     * Descarregue todos os plugins
     */
    void unloadPlugins();

    /**
     * Descarregue um archbasePlugin.
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @return true se o archbasePlugin foi descarregado
     * @throws PluginRuntimeException se algo der errado
     */
    boolean unloadPlugin(String pluginId);

    /**
     * Desativa o carregamento de um archbasePlugin.
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @return true se o archbasePlugin estiver desabilitado
     * @throws PluginRuntimeException se algo der errado
     */
    boolean disablePlugin(String pluginId);

    /**
     * Ativa um archbasePlugin que foi desativado anteriormente.
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @return true se o archbasePlugin estiver habilitado
     * @throws PluginRuntimeException se algo der errado
     */
    boolean enablePlugin(String pluginId);

    /**
     * Exclui um archbasePlugin.
     *
     * @param pluginId o identificador exclusivo do archbasePlugin, especificado em seus metadados
     * @return true se o archbasePlugin foi excluído
     * @throws PluginRuntimeException se algo der errado
     */
    boolean deletePlugin(String pluginId);

    ClassLoader getPluginClassLoader(String pluginId);

    List<Class<?>> getExtensionClasses(String pluginId);

    <T> List<Class<? extends T>> getExtensionClasses(Class<T> type);

    <T> List<Class<? extends T>> getExtensionClasses(Class<T> type, String pluginId);

    <T> List<T> getExtensions(Class<T> type);

    <T> List<T> getExtensions(Class<T> type, String pluginId);

    List<Object> getExtensions(String pluginId);

    Set<String> getExtensionClassNames(String pluginId);

    ExtensionFactory getExtensionFactory();

    /**
     * O modo de tempo de execução. Atualmente, deve ser DEVELOPMENT ou DEPLOYMENT.
     */
    RuntimeMode getRuntimeMode();

    /**
     * Retorna {@code true} se o modo de tempo de execução é {@code RuntimeMode.DEVELOPMENT}.
     */
    default boolean isDevelopment() {
        return RuntimeMode.DEVELOPMENT.equals(getRuntimeMode());
    }

    /**
     * Retorna {@code true} se o modo de tempo de execução não for {@code RuntimeMode.DEVELOPMENT}.
     */
    default boolean isNotDevelopment() {
        return !isDevelopment();
    }

    /**
     * Recupera o {@link PluginWrapper} que carregou a classe 'clazz' fornecida.
     */
    PluginWrapper whichPlugin(Class<?> clazz);

    void addPluginStateListener(PluginStateListener listener);

    void removePluginStateListener(PluginStateListener listener);

    /**
     * Retorna a versão do sistema.
     *
     * @return a versão do sistema
     */
    String getSystemVersion();

    /**
     * Defina a versão do sistema. Isso é usado para comparar com o archbasePlugin
     * requer atributo. A versão padrão do sistema é 0.0.0, que
     * desativa todas as verificações de versão.
     *
     * @param version
     * @default 0.0.0
     */
    void setSystemVersion(String version);

    /**
     * Obtém uma lista somente leitura de todos os caminhos das pastas onde os plug-ins estão instalados.
     *
     * @return Caminhos de raízes de plug-ins
     */
    List<Path> getPluginsRoots();

    /**
     * Gets the first path of the folders where plugins are installed.
     *
     * @return Path of plugins root
     */
    Path getPluginsRoot();

    VersionManager getVersionManager();

}
