package br.com.archbase.modulith.communication.contracts;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Interface base para eventos de integração entre módulos.
 * <p>
 * Integration Events são eventos publicados para comunicação entre módulos,
 * diferente de Domain Events que são internos a um bounded context.
 * <p>
 * Características importantes:
 * <ul>
 *   <li>Imutáveis - uma vez criados, não devem ser modificados</li>
 *   <li>Serializáveis - devem poder ser persistidos no Outbox</li>
 *   <li>Versionados - para suportar evolução de contrato</li>
 *   <li>Auto-contidos - contém todas as informações necessárias</li>
 * </ul>
 * <p>
 * Recomenda-se usar Java Records para implementar Integration Events:
 * <pre>
 * {@code
 * @IntegrationEvent(aggregateType = "Order", version = 1)
 * public record OrderCreatedEvent(
 *     String orderId,
 *     String customerId,
 *     BigDecimal totalAmount,
 *     Instant createdAt
 * ) implements IntegrationEvent {
 *     @Override
 *     public String getAggregateId() { return orderId; }
 *
 *     @Override
 *     public String getAggregateType() { return "Order"; }
 * }
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public interface IntegrationEvent extends Serializable {

    /**
     * Retorna o ID único do evento.
     * Por padrão, gera um UUID.
     */
    default String getEventId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Retorna o ID do agregado que originou o evento.
     */
    String getAggregateId();

    /**
     * Retorna o tipo do agregado que originou o evento.
     */
    String getAggregateType();

    /**
     * Retorna o timestamp de quando o evento ocorreu.
     */
    default Instant getOccurredAt() {
        return Instant.now();
    }

    /**
     * Retorna o nome do módulo que publicou o evento.
     */
    default String getSourceModule() {
        return null;
    }

    /**
     * Retorna a versão do schema do evento.
     */
    default int getVersion() {
        return 1;
    }

    /**
     * Retorna o nome do tipo do evento.
     * Por padrão, usa o nome simples da classe.
     */
    default String getEventType() {
        return getClass().getSimpleName();
    }
}
