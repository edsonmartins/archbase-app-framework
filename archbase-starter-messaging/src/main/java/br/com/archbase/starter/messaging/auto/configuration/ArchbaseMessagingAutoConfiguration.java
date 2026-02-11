package br.com.archbase.starter.messaging.auto.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration para aplicações de messaging/workers usando Archbase.
 *
 * Este starter é uma alternativa leve ao archbase-starter-core para aplicações
 * que não precisam de:
 * - Spring Security / OAuth2
 * - JPA / Hibernate
 * - Swagger / SpringDoc
 * - ModelMapper
 *
 * Ideal para:
 * - Workers que processam filas (JMS, RabbitMQ, Kafka)
 * - Microserviços de notificação (email, WhatsApp, SMS)
 * - Processadores batch
 * - Schedulers leves
 *
 * Inclui automaticamente:
 * - Error handling do Archbase
 * - Validação do Archbase
 * - Event-driven specifications
 * - Logging estruturado
 * - ObjectMapper configurado para Java 8+ date/time
 */
@AutoConfiguration
@ComponentScan(basePackages = {
    "br.com.archbase.error.handling",
    "br.com.archbase.validation",
    "br.com.archbase.logging"
})
public class ArchbaseMessagingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseMessagingAutoConfiguration.class);

    /**
     * ObjectMapper configurado para serialização/deserialização JSON
     * com suporte completo a Java 8+ date/time types.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper archbaseMessagingObjectMapper() {
        log.info("Configurando ObjectMapper para Archbase Messaging");

        ObjectMapper mapper = new ObjectMapper();

        // Módulos para tipos Java 8+
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());

        // Configurações de serialização
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Configurações de deserialização
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        return mapper;
    }
}
