package br.com.archbase.plugin.manager.update;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface para verificar um arquivo.
 */
public interface FileVerifier {

    /**
     * Verifica o lançamento de um plugin de acordo com certas regras
     *
     * @param context o objeto de contexto do verificador de arquivo
     * @param file    o caminho para o próprio arquivo baixado
     * @throws IOException     se houver um problema ao acessar o arquivo
     * @throws VerifyException em caso de problemas ao verificar o arquivo
     */
    void verify(Context context, Path file) throws IOException;

    /**
     * Contexto a ser passado para verificadores de arquivo
     */
    class Context {

        protected String id;
        protected Date date;
        protected String version;
        protected String requires;
        protected String url;
        protected String sha512sum;
        protected Map<String, Object> meta = new HashMap<>();

        public Context(String id, PluginInfo.PluginRelease pluginRelease) {
            this.id = id;
            this.date = pluginRelease.date;
            this.version = pluginRelease.version;
            this.requires = pluginRelease.requires;
            this.url = pluginRelease.url;
            this.sha512sum = pluginRelease.sha512sum;
        }

        public Context(String id, Date date, String version, String requires, String url, String sha512sum) {
            this.id = id;
            this.date = date;
            this.version = version;
            this.requires = requires;
            this.url = url;
            this.sha512sum = sha512sum;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getRequires() {
            return requires;
        }

        public void setRequires(String requires) {
            this.requires = requires;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSha512sum() {
            return sha512sum;
        }

        public void setSha512sum(String sha512sum) {
            this.sha512sum = sha512sum;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }
    }

}
