package br.com.archbase.starter.hypersistence.auto.configuration;

import br.com.archbase.hypersistence.config.ArchbaseHypersistenceConfiguration;
import br.com.archbase.hypersistence.config.ArchbaseHypersistenceProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuração Spring Boot para o módulo Archbase Hypersistence.
 * <p>
 * Esta auto-configuração é ativada automaticamente quando:
 * </p>
 * <ul>
 *   <li>A classe {@link JsonType} está presente no classpath</li>
 *   <li>A propriedade {@code archbase.hypersistence.enabled} é {@code true} (padrão)</li>
 * </ul>
 *
 * <h3>Funcionalidades configuradas automaticamente:</h3>
 * <ul>
 *   <li>Registro de tipos JSON do Hypersistence Utils</li>
 *   <li>Configuração de propriedades via {@code application.yml}</li>
 *   <li>Integração com repositórios JPA</li>
 * </ul>
 *
 * <h3>Para usar:</h3>
 * <p>
 * Adicione a dependência ao seu projeto:
 * </p>
 * <pre>{@code
 * <dependency>
 *     <groupId>br.com.archbase</groupId>
 *     <artifactId>archbase-starter-hypersistence</artifactId>
 *     <version>${archbase.version}</version>
 * </dependency>
 * }</pre>
 *
 * <p>
 * A configuração é feita automaticamente. Para customizar:
 * </p>
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
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass(JsonType.class)
@ConditionalOnProperty(
        prefix = "archbase.hypersistence",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(ArchbaseHypersistenceProperties.class)
@Import(ArchbaseHypersistenceConfiguration.class)
public class ArchbaseHypersistenceAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ArchbaseHypersistenceAutoConfiguration.class);

    /**
     * Bean de configuração que loga as propriedades ativas.
     *
     * @param properties Propriedades de configuração do Hypersistence
     * @return As propriedades configuradas
     */
    @Bean
    public ArchbaseHypersistenceProperties archbaseHypersistenceProperties(
            ArchbaseHypersistenceProperties properties) {

        if (LOG.isInfoEnabled()) {
            LOG.info("Archbase Hypersistence auto-configuration initialized");
            LOG.info("  - JSON types enabled: {}", properties.getJson().isEnabled());
            LOG.info("  - PostgreSQL array types enabled: {}", properties.getPostgresql().isArrayTypesEnabled());
            LOG.info("  - PostgreSQL range types enabled: {}", properties.getPostgresql().isRangeTypesEnabled());
            LOG.info("  - Enhanced repository methods enabled: {}", properties.getRepository().isEnhancedMethodsEnabled());
        }

        return properties;
    }
}
