package br.com.archbase.plugin.manager.update;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * {@code PluginInfo} descrevendo um plug-in de um repositório.
 */
@SuppressWarnings("all")
public class PluginInfo implements Serializable, Comparable<PluginInfo> {

    public String id;
    public String name;
    public String description;
    public String provider;
    public String projectUrl;
    public List<PluginRelease> releases;

    // São metadados adicionados no momento da análise, não fazem parte do plugins.json publicado
    private String repositoryId;

    @Override
    public int compareTo(PluginInfo o) {
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginInfo that = (PluginInfo) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(provider, that.provider) &&
                Objects.equals(projectUrl, that.projectUrl) &&
                Objects.equals(releases, that.releases) &&
                Objects.equals(repositoryId, that.repositoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, provider, projectUrl, releases, repositoryId);
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public String toString() {
        return "PluginInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", provider='" + provider + '\'' +
                ", projectUrl='" + projectUrl + '\'' +
                ", releases=" + releases +
                ", repositoryId='" + repositoryId + '\'' +
                '}';
    }

    /**
     * Um lançamento concreto.
     */
    @SuppressWarnings("all")
    public static class PluginRelease implements Serializable {

        public String version;
        public Date date;
        public String requires;
        public String url;

        /**
         * Optional sha512 digest checksum. Can be one of
         * <ul>
         *   <li>&lt;sha512 sum string&gt;</li>
         *   <li>URL para um arquivo sha512 externo</li>
         *   <li>".sha512" como um atalho para dizer baixe um arquivo &lt;filename&gt; .sha512 próximo ao arquivo zip/jar</li>
         * </ul>
         */
        protected String sha512sum;

        @Override
        public String toString() {
            return "PluginRelease{" +
                    "version='" + version + '\'' +
                    ", date=" + date +
                    ", requires='" + requires + '\'' +
                    ", url='" + url + '\'' +
                    ", sha512sum='" + sha512sum + '\'' +
                    '}';
        }

    }

}
