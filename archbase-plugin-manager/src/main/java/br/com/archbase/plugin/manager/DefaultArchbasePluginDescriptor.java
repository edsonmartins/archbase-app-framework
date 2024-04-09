package br.com.archbase.plugin.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DefaultArchbasePluginDescriptor implements PluginDescriptor {

    private String pluginId;
    private String pluginDescription;
    private String pluginClass = ArchbasePlugin.class.getName();
    private String version;
    private String requires = "*"; // SemVer format
    private String provider;
    private List<PluginDependency> dependencies;
    private String license;

    public DefaultArchbasePluginDescriptor() {
        dependencies = new ArrayList<>();
    }

    public DefaultArchbasePluginDescriptor(String pluginId, String pluginDescription, String pluginClass, String version, String requires, String provider, String license) {
        this();
        this.pluginId = pluginId;
        this.pluginDescription = pluginDescription;
        this.pluginClass = pluginClass;
        this.version = version;
        this.requires = requires;
        this.provider = provider;
        this.license = license;
    }

    public void addDependency(PluginDependency dependency) {
        this.dependencies.add(dependency);
    }

    /**
     * Retorna o identificador único deste archbasePlugin.
     */
    @Override
    public String getPluginId() {
        return pluginId;
    }

    protected DefaultArchbasePluginDescriptor setPluginId(String pluginId) {
        this.pluginId = pluginId;

        return this;
    }

    /**
     * Retorna a descrição deste archbasePlugin.
     */
    @Override
    public String getPluginDescription() {
        return pluginDescription;
    }

    /**
     * Retorna o nome da classe que implementa a interface ArchbasePlugin.
     */
    @Override
    public String getPluginClass() {
        return pluginClass;
    }

    /**
     * Retorna a versão deste archbasePlugin.
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Retorna a versão da string de requer
     *
     * @return String com requer expressão no formato SemVer
     */
    @Override
    public String getRequires() {
        return requires;
    }

    /**
     * Retorna o nome do provedor deste archbasePlugin.
     */
    @Override
    public String getProvider() {
        return provider;
    }

    /**
     * Retorna a licença legal deste archbasePlugin, por exemplo "Apache-2.0", "MIT" etc.
     */
    @Override
    public String getLicense() {
        return license;
    }

    /**
     * Retorna todas as dependências declaradas por este archbasePlugin.
     * Retorna um array vazio se este archbasePlugin não declara nenhum requerimento.
     */
    @Override
    public List<PluginDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        return "PluginDescriptor [pluginId=" + pluginId + ", pluginClass="
                + pluginClass + ", version=" + version + ", provider="
                + provider + ", dependencies=" + dependencies + ", description="
                + pluginDescription + ", requires=" + requires + ", license="
                + license + "]";
    }

    protected PluginDescriptor setPluginDescription(String pluginDescription) {
        this.pluginDescription = pluginDescription;

        return this;
    }

    protected PluginDescriptor setPluginClass(String pluginClassName) {
        this.pluginClass = pluginClassName;

        return this;
    }


    protected DefaultArchbasePluginDescriptor setPluginVersion(String version) {
        this.version = version;

        return this;
    }

    protected PluginDescriptor setProvider(String provider) {
        this.provider = provider;

        return this;
    }

    protected PluginDescriptor setRequires(String requires) {
        this.requires = requires;

        return this;
    }

    protected PluginDescriptor setDependencies(String dependencies) {
        this.dependencies = new ArrayList<>();

        if (dependencies != null) {
            dependencies = dependencies.trim();
            if (!dependencies.isEmpty()) {
                String[] tokens = dependencies.split(",");
                for (String dependency : tokens) {
                    dependency = dependency.trim();
                    if (!dependency.isEmpty()) {
                        this.dependencies.add(new PluginDependency(dependency));
                    }
                }
            }
        }

        return this;
    }

    public PluginDescriptor setLicense(String license) {
        this.license = license;

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultArchbasePluginDescriptor)) return false;
        DefaultArchbasePluginDescriptor that = (DefaultArchbasePluginDescriptor) o;
        return Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(pluginDescription, that.pluginDescription) &&
                Objects.equals(pluginClass, that.pluginClass) &&
                Objects.equals(version, that.version) &&
                Objects.equals(requires, that.requires) &&
                Objects.equals(provider, that.provider) &&
                dependencies.equals(that.dependencies) &&
                Objects.equals(license, that.license);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, pluginDescription, pluginClass, version, requires, provider, dependencies, license);
    }
}
