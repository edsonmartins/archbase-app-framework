package br.com.archbase.plugin.manager;

import java.util.EventObject;


public class PluginStateEvent extends EventObject {

    private transient PluginWrapper plugin;
    private PluginState oldState;

    public PluginStateEvent(ArchbasePluginManager source, PluginWrapper plugin, PluginState oldState) {
        super(source);

        this.plugin = plugin;
        this.oldState = oldState;
    }

    @Override
    public ArchbasePluginManager getSource() {
        return (ArchbasePluginManager) super.getSource();
    }

    public PluginWrapper getPlugin() {
        return plugin;
    }

    public PluginState getPluginState() {
        return plugin.getPluginState();
    }

    public PluginState getOldState() {
        return oldState;
    }

    @Override
    public String toString() {
        return "PluginStateEvent [archbasePlugin=" + plugin.getPluginId() +
                ", newState=" + getPluginState() +
                ", oldState=" + oldState +
                ']';
    }

}
