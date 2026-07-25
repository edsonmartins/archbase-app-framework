package br.com.archbase.starter.flyway.auto.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades das convenções Flyway do Archbase.
 *
 * <p>Prefixo: {@code archbase.flyway}. Estas convenções complementam (e podem ser sobrescritas por)
 * as propriedades padrão {@code spring.flyway.*} do Spring Boot.
 */
@ConfigurationProperties(prefix = "archbase.flyway")
public class ArchbaseFlywayProperties {

    /** Habilita as convenções Flyway do Archbase. Padrão: {@code true}. */
    private boolean enabled = true;

    /**
     * Faz baseline automático quando o schema já existe (adoção de Flyway em base legada).
     * Padrão: {@code true}.
     */
    private boolean baselineOnMigrate = true;

    /**
     * Versão de baseline aplicada ao schema existente. Padrão {@code "0"} para que a primeira
     * migração versionada ({@code V1__...}) ainda seja executada sobre a base já existente.
     */
    private String baselineVersion = "0";

    /**
     * Acrescenta {@code classpath:db/migration/archbase} às locations do projeto, para que a
     * evolução dos schemas dos próprios módulos do framework (ex.: as tabelas de segurança) venha
     * junto da dependência, em vez de ser redescoberta em cada projeto quando a aplicação deixa de
     * subir. As migrações de lá são {@code R__} idempotentes. Padrão: {@code true}.
     */
    private boolean includeArchbaseLocations = true;

    public boolean isIncludeArchbaseLocations() {
        return includeArchbaseLocations;
    }

    public void setIncludeArchbaseLocations(boolean includeArchbaseLocations) {
        this.includeArchbaseLocations = includeArchbaseLocations;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBaselineOnMigrate() {
        return baselineOnMigrate;
    }

    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    public String getBaselineVersion() {
        return baselineVersion;
    }

    public void setBaselineVersion(String baselineVersion) {
        this.baselineVersion = baselineVersion;
    }
}
