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
 * Traz como dependência o error handling, a validação, as especificações event-driven e o logging estruturado do
 * Archbase; cada um se registra pela própria auto-configuração. Esta classe só acrescenta um {@code ObjectMapper}.
 * O tratamento de erros só existe em aplicação web servlet — num worker sem web não há o que tratar.
 *
 * <p>Até a 3.5.0 esta classe fazia {@code @ComponentScan} de {@code br.com.archbase.error.handling},
 * {@code ...validation} e {@code ...logging}. Os dois primeiros já têm auto-configuração e o terceiro não tem
 * componentes; a varredura registrava o {@code ErrorHandlingControllerAdvice} por fora da configuração dele, e uma
 * aplicação web com este starter não subia ({@code NoSuchBeanDefinitionException: ArchbaseErrorHandlingProperties}).
 */
@AutoConfiguration
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
