package br.com.archbase.modulith.communication;

import br.com.archbase.modulith.communication.contracts.IntegrationEvent;
import jakarta.persistence.EntityManager;

import java.util.function.Consumer;

/**
 * Bus para publicação e assinatura de eventos de integração entre módulos.
 * <p>
 * O IntegrationEventBus fornece comunicação assíncrona entre módulos
 * usando o padrão Outbox para garantia de entrega.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * // Publicar evento
 * integrationEventBus.publish(new OrderCreatedEvent(orderId, customerId));
 *
 * // Publicar com garantia (transacional)
 * integrationEventBus.publishReliably(event, entityManager);
 *
 * // Assinar eventos
 * integrationEventBus.subscribe(OrderCreatedEvent.class, event -> {
 *     // processar evento
 * });
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public interface IntegrationEventBus {

    /**
     * Publica um evento de integração.
     * <p>
     * O evento é publicado de forma assíncrona para todos os subscribers.
     *
     * @param event Evento a ser publicado
     * @param <T>   Tipo do evento
     */
    <T extends IntegrationEvent> void publish(T event);

    /**
     * Publica um evento de integração com garantia de entrega.
     * <p>
     * O evento é persistido na tabela Outbox dentro da transação atual,
     * garantindo que seja entregue mesmo em caso de falha.
     *
     * @param event         Evento a ser publicado
     * @param entityManager EntityManager da transação atual
     * @param <T>           Tipo do evento
     */
    <T extends IntegrationEvent> void publishReliably(T event, EntityManager entityManager);

    /**
     * Publica múltiplos eventos de integração.
     *
     * @param events Eventos a serem publicados
     */
    void publishAll(Iterable<? extends IntegrationEvent> events);

    /**
     * Assina um tipo de evento.
     *
     * @param eventType Classe do tipo de evento
     * @param handler   Handler que processará os eventos
     * @param <T>       Tipo do evento
     */
    <T extends IntegrationEvent> void subscribe(Class<T> eventType, Consumer<T> handler);

    /**
     * Assina um tipo de evento com filtro por módulo de origem.
     *
     * @param eventType    Classe do tipo de evento
     * @param sourceModule Nome do módulo de origem (null para qualquer)
     * @param handler      Handler que processará os eventos
     * @param <T>          Tipo do evento
     */
    <T extends IntegrationEvent> void subscribe(Class<T> eventType, String sourceModule, Consumer<T> handler);

    /**
     * Remove uma assinatura.
     *
     * @param eventType Classe do tipo de evento
     * @param handler   Handler a ser removido
     * @param <T>       Tipo do evento
     */
    <T extends IntegrationEvent> void unsubscribe(Class<T> eventType, Consumer<T> handler);

    /**
     * Remove todas as assinaturas de um tipo de evento.
     *
     * @param eventType Classe do tipo de evento
     * @param <T>       Tipo do evento
     */
    <T extends IntegrationEvent> void unsubscribeAll(Class<T> eventType);
}
