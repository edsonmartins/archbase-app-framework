package br.com.archbase.hypersistence.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração principal do módulo Archbase Hypersistence.
 * <p>
 * Esta configuração é ativada automaticamente quando a propriedade
 * {@code archbase.hypersistence.enabled} é {@code true} (valor padrão).
 * </p>
 *
 * <p>Para desabilitar completamente o módulo:</p>
 * <pre>
 * archbase:
 *   hypersistence:
 *     enabled: false
 * </pre>
 *
 * @author Archbase Team
 * @since 2.1.0
 */
@Configuration
@ConditionalOnProperty(
        prefix = "archbase.hypersistence",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(ArchbaseHypersistenceProperties.class)
public class ArchbaseHypersistenceConfiguration {

}
