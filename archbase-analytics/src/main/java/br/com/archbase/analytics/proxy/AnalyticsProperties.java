package br.com.archbase.analytics.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do host-side da camada semântica. Prefixo
 * {@code archbase.analytics}.
 */
@ConfigurationProperties(prefix = "archbase.analytics")
public class AnalyticsProperties {

    /** Liga o proxy e a autoconfiguração. */
    private boolean enabled = false;

    /** URL interna do Cube (o Cube nunca é exposto ao navegador). */
    private String cubeUrl = "http://cube-api:4000/cubejs-api";

    /**
     * Segredo HS256 compartilhado com o Cube (≥ 32 bytes). O proxy cunha o
     * token de escopo e o assina com este segredo; o Cube o valida com o mesmo.
     */
    private String secret = "";

    /** TTL do token cunhado, em segundos. Curto por decisão de segurança. */
    private int tokenTtlSeconds = 120;

    /** Teto de tempo da consulta no proxy (deve ser MAIOR que o do Cube). */
    private int timeoutSeconds = 30;

    /** Teto de linhas retornadas; acima disso, sinaliza truncamento. */
    private int rowLimit = 50000;

    /** Consultas simultâneas por usuário. */
    private int concurrencyPerUser = 2;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCubeUrl() { return cubeUrl; }
    public void setCubeUrl(String cubeUrl) { this.cubeUrl = cubeUrl; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public int getTokenTtlSeconds() { return tokenTtlSeconds; }
    public void setTokenTtlSeconds(int v) { this.tokenTtlSeconds = v; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
    public int getRowLimit() { return rowLimit; }
    public void setRowLimit(int v) { this.rowLimit = v; }
    public int getConcurrencyPerUser() { return concurrencyPerUser; }
    public void setConcurrencyPerUser(int v) { this.concurrencyPerUser = v; }
}
