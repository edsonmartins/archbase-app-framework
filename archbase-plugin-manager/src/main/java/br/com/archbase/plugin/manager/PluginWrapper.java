package br.com.archbase.plugin.manager;

import java.nio.file.Path;

/**
 * Um wrapper sobre a instância do archbasePlugin.
 */
public class PluginWrapper {

    ArchbasePlugin archbasePlugin; // cache
    private ArchbasePluginManager archbasePluginManager;
    private PluginDescriptor descriptor;
    private Path pluginPath;
    private ClassLoader pluginClassLoader;
    private ArchbasePluginFactory archbasePluginFactory;
    private PluginState pluginState;
    private RuntimeMode runtimeMode;
    private Throwable failedException;

    public PluginWrapper(ArchbasePluginManager archbasePluginManager, PluginDescriptor descriptor, Path pluginPath, ClassLoader pluginClassLoader) {
        this.archbasePluginManager = archbasePluginManager;
        this.descriptor = descriptor;
        this.pluginPath = pluginPath;
        this.pluginClassLoader = pluginClassLoader;

        pluginState = PluginState.CREATED;
        runtimeMode = archbasePluginManager.getRuntimeMode();
    }

    /**
     * Retorna o gerenciador de plugins.
     */
    public ArchbasePluginManager getPluginManager() {
        return archbasePluginManager;
    }

    /**
     * Retorna o descritor do archbasePlugin.
     */
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Retorna o caminho deste archbasePlugin.
     */
    public Path getPluginPath() {
        return pluginPath;
    }

    /**
     * Retorna o carregador de classes do archbasePlugin usado para carregar classes e recursos
     * para este plug-in. O carregador de classes pode ser usado para acessar diretamente
     * Recursos e classes de plug-ins.
     */
    public ClassLoader getPluginClassLoader() {
        return pluginClassLoader;
    }

    public ArchbasePlugin getPlugin() {
        if (archbasePlugin == null) {
            archbasePlugin = archbasePluginFactory.create(this);
        }

        return archbasePlugin;
    }

    public PluginState getPluginState() {
        return pluginState;
    }

    public void setPluginState(PluginState pluginState) {
        this.pluginState = pluginState;
    }

    public RuntimeMode getRuntimeMode() {
        return runtimeMode;
    }

    /**
     * Atalho
     */
    public String getPluginId() {
        return getDescriptor().getPluginId();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + descriptor.getPluginId().hashCode();

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        PluginWrapper other = (PluginWrapper) obj;

        return descriptor.getPluginId().equals(other.descriptor.getPluginId());

    }

    @Override
    public String toString() {
        return "PluginWrapper [descriptor=" + descriptor + ", pluginPath=" + pluginPath + "]";
    }

    public void setPluginFactory(ArchbasePluginFactory archbasePluginFactory) {
        this.archbasePluginFactory = archbasePluginFactory;
    }

    /**
     * Retorna a exceção com a qual o plug-in falha ao iniciar.
     * Veja @ {link PluginStatus#FAILED}.
     */
    public Throwable getFailedException() {
        return failedException;
    }

    public void setFailedException(Throwable failedException) {
        this.failedException = failedException;
    }

}
