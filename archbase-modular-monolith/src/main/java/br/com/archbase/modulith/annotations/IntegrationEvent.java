package br.com.archbase.modulith.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma classe como um Integration Event para comunicação entre módulos.
 * <p>
 * Integration Events diferem de Domain Events:
 * - Domain Events: internos ao bounded context, podem conter objetos de domínio
 * - Integration Events: externos, usam tipos primitivos/DTOs, são imutáveis
 * <p>
 * Integration Events devem ser:
 * - Imutáveis (records ou classes com campos finais)
 * - Serializáveis (JSON)
 * - Pequenos (apenas dados essenciais)
 * - Versionados
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @IntegrationEvent(aggregateType = "Order", version = 1)
 * public record OrderCreatedEvent(
 *     String orderId,
 *     String customerId,
 *     BigDecimal totalAmount,
 *     Instant createdAt
 * ) implements br.com.archbase.modulith.communication.contracts.IntegrationEvent {}
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IntegrationEvent {

    /**
     * Tipo do agregado que originou o evento.
     * Usado para particionamento e ordenação de eventos.
     */
    String aggregateType();

    /**
     * Versão do schema do evento.
     * Permite evolução do schema mantendo compatibilidade.
     */
    int version() default 1;

    /**
     * Nome do módulo que publica este evento.
     * Se vazio, será inferido do pacote.
     */
    String sourceModule() default "";

    /**
     * Indica se o evento deve ser persistido no outbox para garantia de entrega.
     */
    boolean reliable() default true;

    /**
     * Tempo de retenção do evento no outbox em minutos.
     * Após este período, o evento pode ser removido.
     */
    int retentionMinutes() default 1440; // 24 horas
}
