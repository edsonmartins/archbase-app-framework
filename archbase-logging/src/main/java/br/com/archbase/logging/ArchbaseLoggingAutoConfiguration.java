package br.com.archbase.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-configuração para logging estruturado do Archbase.
 * <p>
 * Habilitar via application.properties:
 * <pre>
 * archbase.logging.enabled=true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "archbase.logging", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ArchbaseLoggingProperties.class)
public class ArchbaseLoggingAutoConfiguration {

    @Configuration
    @ConditionalOnWebApplication
    @ConditionalOnClass(jakarta.servlet.Filter.class)
    @ConditionalOnProperty(prefix = "archbase.logging", name = "correlation-id-filter-enabled", havingValue = "true", matchIfMissing = true)
    static class CorrelationIdFilterConfiguration {

        @Bean
        public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(
                ArchbaseLoggingProperties properties) {

            String headerName = properties.correlationHeader() != null
                    ? properties.correlationHeader()
                    : "X-Correlation-ID";
            CorrelationIdFilter filter = new CorrelationIdFilter(headerName);

            FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            registration.setName("CorrelationIdFilter");

            return registration;
        }
    }
}
