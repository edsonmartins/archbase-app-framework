package br.com.archbase.starter.messaging.auto.configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
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

        return JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .build();
    }
}
