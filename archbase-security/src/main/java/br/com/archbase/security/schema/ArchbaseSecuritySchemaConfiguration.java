package br.com.archbase.security.schema;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Registra a rotina que mantém o esquema de segurança em dia.
 *
 * <p><b>Sobre a ausência de {@code @ConditionalOnSingleCandidate(DataSource.class)}.</b> Seria a
 * anotação óbvia para "só registre se houver um único DataSource", e ela <b>não funciona aqui</b>:
 * as condições que olham para beans são avaliadas quando a classe de configuração é processada, e
 * esta é uma {@code @Configuration} comum, encontrada por varredura — portanto processada
 * <i>antes</i> das auto-configurações que criam o DataSource. A condição via sempre zero candidatos
 * e o bean nunca era registrado. A decisão passou para dentro do inicializador, onde o contexto já
 * está completo.
 *
 * @see ArchbaseSecuritySchemaInitializer
 */
@Configuration
@EnableConfigurationProperties(ArchbaseSecuritySchemaProperties.class)
public class ArchbaseSecuritySchemaConfiguration {

    @Bean
    @ConditionalOnMissingBean(ArchbaseSecuritySchemaInitializer.class)
    public ArchbaseSecuritySchemaInitializer archbaseSecuritySchemaInitializer(
            ObjectProvider<DataSource> dataSource,
            ObjectProvider<EntityManagerFactory> entityManagerFactory,
            ArchbaseSecuritySchemaProperties properties) {
        return new ArchbaseSecuritySchemaInitializer(dataSource, entityManagerFactory, properties);
    }
}
