package br.com.archbase.hypersistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração para o módulo Archbase Hypersistence.
 * <p>
 * Permite configurar funcionalidades avançadas de persistência como
 * tipos JSON, arrays PostgreSQL e métodos de repositório otimizados.
 * </p>
 *
 * <p>Exemplo de configuração no application.yml:</p>
 * <pre>
 * archbase:
 *   hypersistence:
 *     enabled: true
 *     json:
 *       enabled: true
 *     postgresql:
 *       array-types-enabled: true
 *       range-types-enabled: true
 *     repository:
 *       enhanced-methods-enabled: false
 * </pre>
 *
 * @author Archbase Team
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "archbase.hypersistence")
public class ArchbaseHypersistenceProperties {

    /**
     * Habilita ou desabilita completamente o módulo hypersistence.
     */
    private boolean enabled = true;

    /**
     * Configurações relacionadas a tipos JSON.
     */
    private Json json = new Json();

    /**
     * Configurações específicas para PostgreSQL.
     */
    private PostgreSQL postgresql = new PostgreSQL();

    /**
     * Configurações de repositório.
     */
    private Repository repository = new Repository();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Json getJson() {
        return json;
    }

    public void setJson(Json json) {
        this.json = json;
    }

    public PostgreSQL getPostgresql() {
        return postgresql;
    }

    public void setPostgresql(PostgreSQL postgresql) {
        this.postgresql = postgresql;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * Configurações para tipos JSON.
     */
    public static class Json {

        /**
         * Habilita suporte a tipos JSON.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Configurações específicas para PostgreSQL.
     */
    public static class PostgreSQL {

        /**
         * Habilita tipos de array (text[], int[], uuid[], etc.).
         */
        private boolean arrayTypesEnabled = true;

        /**
         * Habilita tipos de range (daterange, int4range, etc.).
         */
        private boolean rangeTypesEnabled = true;

        /**
         * Habilita tipo HStore (map key-value).
         */
        private boolean hstoreEnabled = false;

        /**
         * Habilita tipo Inet (endereços IP).
         */
        private boolean inetEnabled = false;

        public boolean isArrayTypesEnabled() {
            return arrayTypesEnabled;
        }

        public void setArrayTypesEnabled(boolean arrayTypesEnabled) {
            this.arrayTypesEnabled = arrayTypesEnabled;
        }

        public boolean isRangeTypesEnabled() {
            return rangeTypesEnabled;
        }

        public void setRangeTypesEnabled(boolean rangeTypesEnabled) {
            this.rangeTypesEnabled = rangeTypesEnabled;
        }

        public boolean isHstoreEnabled() {
            return hstoreEnabled;
        }

        public void setHstoreEnabled(boolean hstoreEnabled) {
            this.hstoreEnabled = hstoreEnabled;
        }

        public boolean isInetEnabled() {
            return inetEnabled;
        }

        public void setInetEnabled(boolean inetEnabled) {
            this.inetEnabled = inetEnabled;
        }
    }

    /**
     * Configurações de repositório.
     */
    public static class Repository {

        /**
         * Habilita métodos de repositório otimizados (persist, merge, update).
         * Quando habilitado, os repositórios terão acesso aos métodos do
         * BaseJpaRepository do Hypersistence Utils.
         */
        private boolean enhancedMethodsEnabled = false;

        public boolean isEnhancedMethodsEnabled() {
            return enhancedMethodsEnabled;
        }

        public void setEnhancedMethodsEnabled(boolean enhancedMethodsEnabled) {
            this.enhancedMethodsEnabled = enhancedMethodsEnabled;
        }
    }
}
