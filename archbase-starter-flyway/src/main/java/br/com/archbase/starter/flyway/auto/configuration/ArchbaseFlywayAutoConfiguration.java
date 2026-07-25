package br.com.archbase.starter.flyway.auto.configuration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

/**
 * Auto-configuração das convenções Flyway do Archbase.
 *
 * <p>Basta o projeto adicionar este starter (mais o módulo Flyway do banco, ex. {@code flyway-mysql})
 * e habilitar o Flyway ({@code spring.flyway.enabled=true}) para ganhar, sem configuração extra,
 * as convenções de adoção de Flyway sobre uma base existente:
 * <ul>
 *   <li>{@code baseline-on-migrate=true} — não falha quando o schema já existe;</li>
 *   <li>{@code baseline-version=0} — a primeira migração versionada ({@code V1__...}) executa
 *       mesmo sobre a base já criada (ex.: por {@code ddl-auto}).</li>
 * </ul>
 *
 * <p>Registra um {@link FlywayConfigurationCustomizer}, aplicado pela {@link FlywayAutoConfiguration}.
 * Os valores vêm de {@link ArchbaseFlywayProperties} ({@code archbase.flyway.*}); para desligar as
 * convenções use {@code archbase.flyway.enabled=false} e configure via {@code spring.flyway.*}.
 */
@AutoConfiguration(before = FlywayAutoConfiguration.class)
@ConditionalOnClass({Flyway.class, FlywayConfigurationCustomizer.class})
@ConditionalOnProperty(prefix = "archbase.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ArchbaseFlywayProperties.class)
public class ArchbaseFlywayAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ArchbaseFlywayAutoConfiguration.class);

    /** Onde os módulos do framework publicam a evolução dos próprios schemas. */
    static final String ARCHBASE_LOCATION = "classpath:db/migration/archbase";

    @Bean
    public FlywayConfigurationCustomizer archbaseFlywayConventionsCustomizer(ArchbaseFlywayProperties properties) {
        LOG.info("Archbase Flyway conventions: baselineOnMigrate={}, baselineVersion={}",
                properties.isBaselineOnMigrate(), properties.getBaselineVersion());
        return configuration -> {
            configuration.baselineOnMigrate(properties.isBaselineOnMigrate());
            if (properties.getBaselineVersion() != null && !properties.getBaselineVersion().isBlank()) {
                configuration.baselineVersion(properties.getBaselineVersion());
            }
            if (properties.isIncludeArchbaseLocations()) {
                addArchbaseLocation(configuration);
            }
        };
    }

    /**
     * Acrescenta — nunca substitui — a location das migrações do próprio framework.
     *
     * <p>Sem isto cada projeto precisa descobrir e escrever à mão as colunas que uma nova versão do
     * Archbase passou a exigir, e só descobre quando a aplicação deixa de subir, uma coluna por vez
     * ("Schema validation: missing column ... in table [seguranca]"). As migrações de lá são
     * {@code R__} idempotentes: não entram na numeração do projeto nem falham em base que já as
     * tenha aplicado.</p>
     */
    private void addArchbaseLocation(FluentConfiguration configuration) {
        String[] current = Arrays.stream(configuration.getLocations())
                .map(Location::getDescriptor)
                .toArray(String[]::new);

        if (Arrays.asList(current).contains(ARCHBASE_LOCATION)) {
            return;
        }
        String[] merged = Arrays.copyOf(current, current.length + 1);
        merged[current.length] = ARCHBASE_LOCATION;
        configuration.locations(merged);
        LOG.info("Archbase Flyway: location {} acrescentada às {} já configuradas",
                ARCHBASE_LOCATION, current.length);
    }
}
