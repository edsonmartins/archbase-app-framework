package br.com.archbase.ddd.domain.events;

import br.com.archbase.ddd.domain.annotations.DomainEvent;
import br.com.archbase.ddd.domain.annotations.Externalized;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainEventMetadataTest {

    @DomainEvent(namespace = "vendas", name = "pedido-confirmado")
    @Externalized(target = "vendas.pedidos")
    record PedidoConfirmado() {
    }

    @DomainEvent
    @Externalized
    record PedidoCancelado() {
    }

    @Externalized("legado")
    record SemAnotacaoDeEvento() {
    }

    record Interno() {
    }

    @Test
    void usaOsValoresDeclarados() {
        assertEquals("vendas", DomainEventMetadata.namespace(PedidoConfirmado.class));
        assertEquals("pedido-confirmado", DomainEventMetadata.name(PedidoConfirmado.class));
        assertEquals("vendas.pedido-confirmado", DomainEventMetadata.qualifiedName(PedidoConfirmado.class));
        assertEquals(Optional.of("vendas.pedidos"), DomainEventMetadata.externalTarget(PedidoConfirmado.class));
    }

    @Test
    void padroesSaoPacoteENomeSimples() {
        assertEquals("br.com.archbase.ddd.domain.events", DomainEventMetadata.namespace(PedidoCancelado.class));
        assertEquals("PedidoCancelado", DomainEventMetadata.name(PedidoCancelado.class));
        assertEquals(Optional.of(""), DomainEventMetadata.externalTarget(PedidoCancelado.class));
        assertEquals(Optional.of("legado"), DomainEventMetadata.externalTarget(SemAnotacaoDeEvento.class));
    }

    @Test
    void eventoNaoExternalizado() {
        assertFalse(DomainEventMetadata.isExternalized(Interno.class));
        assertTrue(DomainEventMetadata.isExternalized(PedidoCancelado.class));
        assertEquals(Optional.empty(), DomainEventMetadata.externalTarget(Interno.class));
    }
}
