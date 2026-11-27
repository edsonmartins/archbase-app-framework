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
     * Registra no log que a auto-configuração está ativa.
     *
     * <p>Até a 3.5.0 isto era um {@code @Bean} que devolvia o próprio {@link ArchbaseHypersistenceProperties} — que o
     * {@code @EnableConfigurationProperties} já registra. Ficavam dois beans do mesmo tipo, e injetar as propriedades
     * sem qualificador falhava por ambiguidade. As chaves {@code json.*}, {@code postgresql.*} e {@code repository.*}
     * não ligam nem desligam nada: os tipos do Hypersistence Utils valem sempre que a biblioteca está no classpath.
     */
    public ArchbaseHypersistenceAutoConfiguration(ArchbaseHypersistenceProperties properties) {
        LOG.info("Archbase Hypersistence auto-configuration ativa (json.*, postgresql.* e repository.* são só informativas)");
    }
}
