package br.com.archbase.plugin.manager;

import java.nio.file.Path;

public class PluginAlreadyLoadedException extends PluginRuntimeException {

    private final String pluginId;
    private final transient Path pluginPath;

    public PluginAlreadyLoadedException(String pluginId, Path pluginPath) {
        super("ArchbasePlugin '{} já carregado com id '{}'", pluginPath, pluginId);

        this.pluginId = pluginId;
        this.pluginPath = pluginPath;
    }

    public String getPluginId() {
        return pluginId;
    }

    public Path getPluginPath() {
        return pluginPath;
    }

}
