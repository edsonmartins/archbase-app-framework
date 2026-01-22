package br.com.archbase.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração do logging estruturado.
 */
@ConfigurationProperties(prefix = "archbase.logging")
public class ArchbaseLoggingProperties {

    /**
     * Habilita o logging estruturado.
     */
    private boolean enabled = false;

    /**
     * Habilita o filter de correlation ID.
     */
    private boolean correlationIdFilterEnabled = true;

    /**
     * Nome do header de correlation ID.
     */
    private String correlationHeader = "X-Correlation-ID";

    /**
     * Habilita logging de requisições/respostas HTTP.
     */
    private boolean httpLoggingEnabled = false;

    /**
     * Prefixo para aplicativo no log (ex: "ARCHBASE").
     */
    private String applicationName = "";

    /**
     * Configuração de logging de requisições HTTP.
     */
    private HttpLogging httpLogging = new HttpLogging();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCorrelationIdFilterEnabled() {
        return correlationIdFilterEnabled;
    }

    public void setCorrelationIdFilterEnabled(boolean correlationIdFilterEnabled) {
        this.correlationIdFilterEnabled = correlationIdFilterEnabled;
    }

    public String getCorrelationHeader() {
        return correlationHeader;
    }

    public void setCorrelationHeader(String correlationHeader) {
        this.correlationHeader = correlationHeader;
    }

    public boolean isHttpLoggingEnabled() {
        return httpLoggingEnabled;
    }

    public void setHttpLoggingEnabled(boolean httpLoggingEnabled) {
        this.httpLoggingEnabled = httpLoggingEnabled;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public HttpLogging getHttpLogging() {
        return httpLogging;
    }

    public void setHttpLogging(HttpLogging httpLogging) {
        this.httpLogging = httpLogging;
    }

    public String correlationHeader() {
        return correlationHeader;
    }

    public static class HttpLogging {

        /**
         * Habilita logging do corpo da requisição.
         */
        private boolean logRequestBody = false;

        /**
         * Habilita logging do corpo da resposta.
         */
        private boolean logResponseBody = false;

        /**
         * Tamanho máximo do corpo a ser logado.
         */
        private int maxPayloadSize = 10000;

        /**
         * Headers que não devem ser logados (ex: Authorization).
         */
        private String[] obscuredHeaders = new String[]{
                "authorization", "x-api-key", "secret", "token", "password"
        };

        /**
         * URIs que não devem ser logadas.
         */
        private String[] ignoredUris = new String[]{
                "/actuator/health", "/actuator/info", "/actuator/prometheus"
        };

        // Getters and Setters
        public boolean isLogRequestBody() {
            return logRequestBody;
        }

        public void setLogRequestBody(boolean logRequestBody) {
            this.logRequestBody = logRequestBody;
        }

        public boolean isLogResponseBody() {
            return logResponseBody;
        }

        public void setLogResponseBody(boolean logResponseBody) {
            this.logResponseBody = logResponseBody;
        }

        public int getMaxPayloadSize() {
            return maxPayloadSize;
        }

        public void setMaxPayloadSize(int maxPayloadSize) {
            this.maxPayloadSize = maxPayloadSize;
        }

        public String[] getObscuredHeaders() {
            return obscuredHeaders;
        }

        public void setObscuredHeaders(String[] obscuredHeaders) {
            this.obscuredHeaders = obscuredHeaders;
        }

        public String[] getIgnoredUris() {
            return ignoredUris;
        }

        public void setIgnoredUris(String[] ignoredUris) {
            this.ignoredUris = ignoredUris;
        }
    }
}
