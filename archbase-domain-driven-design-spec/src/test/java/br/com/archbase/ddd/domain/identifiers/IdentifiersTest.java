package br.com.archbase.ddd.domain.identifiers;

import br.com.archbase.ddd.domain.base.ArchbaseIdentifier;
import br.com.archbase.ddd.domain.contracts.Identifier;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifiersTest {

    record OrderId(UUID value) implements Identifier {
    }

    record CustomerCode(String value) implements Identifier {
        CustomerCode {
            if (value.isBlank()) {
                throw new IllegalArgumentException("vazio");
            }
        }
    }

    record TwoParts(String a, String b) implements Identifier {
    }

    record NotAnIdentifier(String value) {
    }

    static final class OrderIdConverter extends AbstractIdentifierAttributeConverter<OrderId, UUID> {
    }

    @Test
    void reconheceSoRecordDeUmComponenteQueImplementaIdentifier() {
        assertTrue(Identifiers.isSingleValue(OrderId.class));
        assertFalse(Identifiers.isSingleValue(TwoParts.class));
        assertFalse(Identifiers.isSingleValue(NotAnIdentifier.class));
        assertFalse(Identifiers.isSingleValue(ArchbaseIdentifier.class), "formato existente não pode mudar");
        assertFalse(Identifiers.isSingleValue(String.class));
        assertThrows(IllegalArgumentException.class, () -> Identifiers.valueOf(new ArchbaseIdentifier("x")));
    }

    @Test
    void embrulhaEDesembrulha() {
        UUID uuid = UUID.randomUUID();
        assertEquals(UUID.class, Identifiers.valueType(OrderId.class));
        assertEquals(uuid, Identifiers.valueOf(new OrderId(uuid)));
        assertEquals(new OrderId(uuid), Identifiers.of(OrderId.class, uuid));
        assertNull(Identifiers.of(OrderId.class, null));
        assertNull(Identifiers.valueOf(null));
    }

    @Test
    void validacaoDoConstrutorCompactoPropaga() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Identifiers.of(CustomerCode.class, " "));
        assertEquals("vazio", e.getMessage());
    }

    @Test
    void conversorJpaResolveOTipoPeloGenerico() {
        UUID uuid = UUID.randomUUID();
        OrderIdConverter converter = new OrderIdConverter();
        assertEquals(uuid, converter.convertToDatabaseColumn(new OrderId(uuid)));
        assertEquals(new OrderId(uuid), converter.convertToEntityAttribute(uuid));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void conversoresSpringConvertemAtravesDoValor() {
        DefaultConversionService service = new DefaultConversionService();
        IdentifierConverters.register(service);
        UUID uuid = UUID.randomUUID();

        assertEquals(new OrderId(uuid), service.convert(uuid.toString(), OrderId.class));
        assertEquals(uuid.toString(), service.convert(new OrderId(uuid), String.class));
        assertEquals(uuid, service.convert(new OrderId(uuid), UUID.class));
        assertEquals(new CustomerCode("abc"), service.convert("abc", CustomerCode.class));
        assertFalse(service.canConvert(String.class, TwoParts.class));
    }
}
