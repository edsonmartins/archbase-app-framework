package br.com.archbase.starter.analytics.auto.configuration;

import br.com.archbase.analytics.port.AnalyticsAuditPort;
import br.com.archbase.analytics.port.DataScopeProvider;
import br.com.archbase.analytics.port.SavedQueryStorePort;
import br.com.archbase.analytics.proxy.AnalyticsProperties;
import br.com.archbase.analytics.proxy.AnalyticsProxyController;
import br.com.archbase.analytics.savedquery.SavedQueryController;
import br.com.archbase.analytics.token.CubeTokenMinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguração do host-side da camada semântica.
 *
 * <p>Liga por {@code archbase.analytics.enabled=true}. Cada peça é
 * {@code @ConditionalOnMissingBean}, então o produto pode sobrescrever qualquer
 * uma.
 *
 * <p><b>O produto DEVE fornecer</b> os beans de porta: um {@link DataScopeProvider}
 * (projeção de escopo), um {@link AnalyticsAuditPort} e um
 * {@link SavedQueryStorePort} (persistência). O proxy só é registrado quando há
 * {@code DataScopeProvider} e {@code AnalyticsAuditPort} — o
 * {@code @ConditionalOnBean} evita subir uma camada semântica sem autorização
 * nem auditoria.
 */
@Configuration
@ConditionalOnProperty(prefix = "archbase.analytics", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AnalyticsProperties.class)
public class ArchbaseAnalyticsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CubeTokenMinter cubeTokenMinter(ObjectMapper objectMapper, AnalyticsProperties props) {
        return new CubeTokenMinter(objectMapper, props.getSecret(), props.getTokenTtlSeconds());
    }

    @Bean
    @ConditionalOnBean({DataScopeProvider.class, AnalyticsAuditPort.class})
    @ConditionalOnMissingBean
    public AnalyticsProxyController analyticsProxyController(
            AnalyticsAuditPort audit, DataScopeProvider scopeProvider,
            CubeTokenMinter minter, ObjectMapper objectMapper, AnalyticsProperties props) {
        return new AnalyticsProxyController(audit, scopeProvider, minter, objectMapper, props);
    }

    @Bean
    @ConditionalOnBean(SavedQueryStorePort.class)
    @ConditionalOnMissingBean
    public SavedQueryController savedQueryController(
            SavedQueryStorePort store, ObjectMapper objectMapper) {
        return new SavedQueryController(store, objectMapper);
    }
}
