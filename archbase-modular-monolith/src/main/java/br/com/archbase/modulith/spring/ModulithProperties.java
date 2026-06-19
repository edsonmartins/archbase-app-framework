package br.com.archbase.modulith.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Propriedades de configuração do Modular Monolith.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@ConfigurationProperties(prefix = "archbase.modulith")
public class ModulithProperties {

    /**
     * Habilita o suporte a Modular Monolith.
     */
    private boolean enabled = true;

    /**
     * Pacotes para scan de módulos.
     */
    private List<String> scanPackages = new ArrayList<>();

    /**
     * Valida dependências na inicialização.
     */
    private boolean validateDependenciesOnStartup = true;

    /**
     * Habilita health checks de módulos.
     */
    private boolean healthChecksEnabled = true;

    /**
     * Habilita métricas de módulos.
     */
    private boolean metricsEnabled = true;

    /**
     * Configurações do Event Bus.
     */
    private EventBusProperties eventBus = new EventBusProperties();

    /**
     * Configurações do Module Gateway.
     */
    private GatewayProperties gateway = new GatewayProperties();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getScanPackages() {
        return scanPackages;
    }

    public void setScanPackages(List<String> scanPackages) {
        this.scanPackages = scanPackages;
    }

    public boolean isValidateDependenciesOnStartup() {
        return validateDependenciesOnStartup;
    }

    public void setValidateDependenciesOnStartup(boolean validateDependenciesOnStartup) {
        this.validateDependenciesOnStartup = validateDependenciesOnStartup;
    }

    public boolean isHealthChecksEnabled() {
        return healthChecksEnabled;
    }

    public void setHealthChecksEnabled(boolean healthChecksEnabled) {
        this.healthChecksEnabled = healthChecksEnabled;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public EventBusProperties getEventBus() {
        return eventBus;
    }

    public void setEventBus(EventBusProperties eventBus) {
        this.eventBus = eventBus;
    }

    public GatewayProperties getGateway() {
        return gateway;
    }

    public void setGateway(GatewayProperties gateway) {
        this.gateway = gateway;
    }

    /**
     * Propriedades do Event Bus.
     */
    public static class EventBusProperties {
        /**
         * Número de threads para processamento de eventos.
         */
        private int threadPoolSize = 10;

        /**
         * Usa Outbox pattern por padrão.
         */
        private boolean useOutbox = true;

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }

        public boolean isUseOutbox() {
            return useOutbox;
        }

        public void setUseOutbox(boolean useOutbox) {
            this.useOutbox = useOutbox;
        }
    }

    /**
     * Propriedades do Gateway.
     */
    public static class GatewayProperties {
        /**
         * Timeout padrão em segundos.
         */
        private int defaultTimeoutSeconds = 30;

        /**
         * Número de threads para processamento de requisições.
         */
        private int threadPoolSize = 10;

        public int getDefaultTimeoutSeconds() {
            return defaultTimeoutSeconds;
        }

        public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) {
            this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }
}
