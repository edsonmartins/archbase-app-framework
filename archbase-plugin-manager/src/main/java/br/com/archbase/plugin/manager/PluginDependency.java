package br.com.archbase.plugin.manager;

import java.util.Objects;


public class PluginDependency {

    private String pluginId;
    private String pluginVersionSupport = "*";
    private boolean optional;

    public PluginDependency(String dependency) {
        int index = dependency.indexOf('@');
        if (index == -1) {
            this.pluginId = dependency;
        } else {
            this.pluginId = dependency.substring(0, index);
            if (dependency.length() > index + 1) {
                this.pluginVersionSupport = dependency.substring(index + 1);
            }
        }

        // Uma dependência é considerada opcional, se o id do archbasePlugin terminar com um ponto de interrogação.
        this.optional = this.pluginId.endsWith("?");
        if (this.optional) {
            this.pluginId = this.pluginId.substring(0, this.pluginId.length() - 1);
        }
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getPluginVersionSupport() {
        return pluginVersionSupport;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        return "PluginDependency [pluginId=" + pluginId + ", pluginVersionSupport="
                + pluginVersionSupport + ", optional="
                + optional + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginDependency)) return false;
        PluginDependency that = (PluginDependency) o;
        return optional == that.optional &&
                pluginId.equals(that.pluginId) &&
                pluginVersionSupport.equals(that.pluginVersionSupport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, pluginVersionSupport, optional);
    }
}
