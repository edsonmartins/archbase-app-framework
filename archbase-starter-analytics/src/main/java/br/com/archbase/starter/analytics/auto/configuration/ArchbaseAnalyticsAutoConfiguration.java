package br.com.archbase.starter.analytics.auto.configuration;

import br.com.archbase.analytics.port.AnalyticsAuditPort;
import br.com.archbase.analytics.port.DataScopeProvider;
import br.com.archbase.analytics.port.SavedQueryStorePort;
import br.com.archbase.analytics.proxy.AnalyticsProperties;
import br.com.archbase.analytics.proxy.AnalyticsProxyController;
import br.com.archbase.analytics.savedquery.SavedQueryController;
import br.com.archbase.analytics.token.CubeSecretValidator;
import br.com.archbase.analytics.token.CubeTokenMinter;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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

    /**
     * O segredo é conferido aqui, e não dentro do minter, porque este é o ponto em que
     * ele deixa de ser configuração e vira chave de assinatura. Ver
     * {@link CubeSecretValidator} para o motivo de o padrão avisar em vez de recusar.
     */
    @Bean
    @ConditionalOnMissingBean
    public CubeTokenMinter cubeTokenMinter(ObjectProvider<ObjectMapper> objectMapper, AnalyticsProperties props) {
        CubeSecretValidator.validar(props);
        return new CubeTokenMinter(mapper(objectMapper), props.getSecret(), props.getTokenTtlSeconds());
    }

    @Bean
    @ConditionalOnBean({DataScopeProvider.class, AnalyticsAuditPort.class})
    @ConditionalOnMissingBean
    public AnalyticsProxyController analyticsProxyController(
            AnalyticsAuditPort audit, DataScopeProvider scopeProvider,
            CubeTokenMinter minter, ObjectProvider<ObjectMapper> objectMapper, AnalyticsProperties props) {
        return new AnalyticsProxyController(audit, scopeProvider, minter, mapper(objectMapper), props);
    }

    @Bean
    @ConditionalOnBean(SavedQueryStorePort.class)
    @ConditionalOnMissingBean
    public SavedQueryController savedQueryController(
            SavedQueryStorePort store, ObjectProvider<ObjectMapper> objectMapper) {
        return new SavedQueryController(store, mapper(objectMapper));
    }

    /**
     * O mapper do Jackson 3 da aplicação (o {@code jsonMapper} do Spring Boot 4); sem ele, um padrão.
     *
     * <p>Até a 3.5.0 o módulo usava o {@code ObjectMapper} do Jackson 2, que o Spring Boot 4 não registra: com
     * {@code archbase.analytics.enabled=true} a aplicação não subia. E as consultas salvas recebiam e devolviam
     * {@code JsonNode} do Jackson 2, que o conversor HTTP do Boot 4 (Jackson 3) não sabe ler nem escrever.
     */
    private static ObjectMapper mapper(ObjectProvider<ObjectMapper> objectMapper) {
        return objectMapper.getIfAvailable(() -> JsonMapper.builder().build());
    }
}
