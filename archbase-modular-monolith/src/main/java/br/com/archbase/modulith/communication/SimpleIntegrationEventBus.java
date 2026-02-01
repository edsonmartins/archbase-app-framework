package br.com.archbase.modulith.communication;

import br.com.archbase.modulith.communication.contracts.IntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Implementação simples do IntegrationEventBus usando memória.
 * <p>
 * Para produção, considere usar uma implementação baseada em
 * message broker (RabbitMQ, Kafka, etc.).
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class SimpleIntegrationEventBus implements IntegrationEventBus {

    private static final Logger log = LoggerFactory.getLogger(SimpleIntegrationEventBus.class);

    private final Map<Class<?>, List<EventSubscription<?>>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;
    private final String currentModule;

    public SimpleIntegrationEventBus(String currentModule) {
        this(currentModule, Executors.newCachedThreadPool(), new ObjectMapper());
    }

    public SimpleIntegrationEventBus(String currentModule, ExecutorService executor, ObjectMapper objectMapper) {
        this.currentModule = currentModule;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IntegrationEvent> void publish(T event) {
        log.debug("Publishing integration event: {} from module: {}",
                event.getEventType(), currentModule);

        List<EventSubscription<?>> eventSubscriptions = subscriptions.get(event.getClass());
        if (eventSubscriptions == null || eventSubscriptions.isEmpty()) {
            log.debug("No subscribers for event type: {}", event.getClass().getSimpleName());
            return;
        }

        for (EventSubscription<?> subscription : eventSubscriptions) {
            if (subscription.sourceModule == null ||
                    subscription.sourceModule.equals(currentModule)) {

                executor.submit(() -> {
                    try {
                        ((Consumer<T>) subscription.handler).accept(event);
                    } catch (Exception e) {
                        log.error("Error processing integration event: {}", event.getEventType(), e);
                    }
                });
            }
        }
    }

    @Override
    public <T extends IntegrationEvent> void publishReliably(T event, EntityManager entityManager) {
        // Persistir no Outbox
        try {
            String payload = objectMapper.writeValueAsString(event);

            // Criar OutboxEvent usando a estrutura existente do archbase-event-driven
            var outboxEvent = new OutboxEventRecord(
                    UUID.randomUUID().toString(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    payload,
                    event.getOccurredAt()
            );

            entityManager.persist(outboxEvent);
            log.debug("Persisted integration event to outbox: {}", event.getEventType());

        } catch (Exception e) {
            log.error("Failed to persist integration event to outbox", e);
            throw new RuntimeException("Failed to persist integration event", e);
        }

        // Também publicar in-memory para processamento imediato
        publish(event);
    }

    @Override
    public void publishAll(Iterable<? extends IntegrationEvent> events) {
        events.forEach(this::publish);
    }

    @Override
    public <T extends IntegrationEvent> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribe(eventType, null, handler);
    }

    @Override
    public <T extends IntegrationEvent> void subscribe(Class<T> eventType, String sourceModule, Consumer<T> handler) {
        subscriptions.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new EventSubscription<>(handler, sourceModule));

        log.debug("Subscribed to event type: {} from module: {}",
                eventType.getSimpleName(), sourceModule != null ? sourceModule : "any");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IntegrationEvent> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<EventSubscription<?>> eventSubscriptions = subscriptions.get(eventType);
        if (eventSubscriptions != null) {
            eventSubscriptions.removeIf(s -> s.handler.equals(handler));
        }
    }

    @Override
    public <T extends IntegrationEvent> void unsubscribeAll(Class<T> eventType) {
        subscriptions.remove(eventType);
    }

    /**
     * Encerra o executor de forma limpa.
     */
    public void shutdown() {
        executor.shutdown();
    }

    private record EventSubscription<T>(Consumer<T> handler, String sourceModule) {
    }

    /**
     * Record interno para persistência no Outbox.
     * Compatível com a estrutura do archbase-event-driven.
     */
    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "outbox_event")
    public static class OutboxEventRecord {

        @jakarta.persistence.Id
        private String id;

        @jakarta.persistence.Column(name = "aggregate_type")
        private String aggregateType;

        @jakarta.persistence.Column(name = "aggregate_id")
        private String aggregateId;

        @jakarta.persistence.Column(name = "type")
        private String type;

        @jakarta.persistence.Column(name = "payload", columnDefinition = "TEXT")
        private String payload;

        @jakarta.persistence.Column(name = "timestamp")
        private java.time.Instant timestamp;

        public OutboxEventRecord() {
        }

        public OutboxEventRecord(String id, String aggregateType, String aggregateId,
                                 String type, String payload, java.time.Instant timestamp) {
            this.id = id;
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }

        public String getId() {
            return id;
        }

        public String getAggregateType() {
            return aggregateType;
        }

        public String getAggregateId() {
            return aggregateId;
        }

        public String getType() {
            return type;
        }

        public String getPayload() {
            return payload;
        }

        public java.time.Instant getTimestamp() {
            return timestamp;
        }
    }
}
