package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um {@link DomainEvent} para ser publicado <em>fora</em> da aplicação (broker, outbox, integração), além de
 * — ou em vez de — ser consumido internamente.
 * <p>
 * É só metadado: nada é publicado por causa desta anotação sozinha. A infraestrutura de publicação consulta
 * {@link br.com.archbase.ddd.domain.events.DomainEventMetadata#externalTarget(Class)} para decidir se e para onde
 * enviar. Portado do jMolecules ({@code org.jmolecules.event.annotation.Externalized}).
 * <pre>{@code
 * @DomainEvent(namespace = "vendas", name = "pedido-confirmado")
 * @Externalized("vendas.pedidos")
 * public record PedidoConfirmado(PedidoId pedido) { }
 * }</pre>
 *
 * @since 3.4.6
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Externalized {

    /**
     * Destino lógico (tópico, fila, canal). Vazio: a infraestrutura escolhe por convenção. Alias de
     * {@link #target()}.
     */
    String value() default "";

    /** Destino lógico. Vazio: a infraestrutura escolhe por convenção. Alias de {@link #value()}. */
    String target() default "";
}
