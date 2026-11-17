package br.com.archbase.ddd.infraestructure.jackson;

import br.com.archbase.ddd.domain.base.ArchbaseIdentifier;
import br.com.archbase.ddd.domain.contracts.Identifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchbaseIdentifierJacksonModuleTest {

    public record OrderId(UUID value) implements Identifier {
    }

    public record Order(OrderId id, List<OrderId> related, String note) {
    }

    private final UUID uuid = UUID.fromString("3f1c2b8e-0000-4000-8000-000000000001");

    @Test
    void semOModuloOFormatoNaoMuda() {
        JsonMapper mapper = JsonMapper.builder().build();
        assertEquals("{\"value\":\"" + uuid + "\"}", mapper.writeValueAsString(new OrderId(uuid)));
    }

    @Test
    void comOModuloSerializaComoOValorEFazIdaEVolta() {
        JsonMapper mapper = JsonMapper.builder().addModule(new ArchbaseIdentifierJacksonModule()).build();
        Order order = new Order(new OrderId(uuid), List.of(new OrderId(uuid)), "x");

        String json = mapper.writeValueAsString(order);

        assertEquals("{\"id\":\"" + uuid + "\",\"related\":[\"" + uuid + "\"],\"note\":\"x\"}", json);
        assertEquals(order, mapper.readValue(json, Order.class));
    }

    @Test
    void archbaseIdentifierNaoEAfetado() {
        JsonMapper plain = JsonMapper.builder().build();
        JsonMapper withModule = JsonMapper.builder().addModule(new ArchbaseIdentifierJacksonModule()).build();
        ArchbaseIdentifier id = new ArchbaseIdentifier("abc");

        assertEquals(plain.writeValueAsString(id), withModule.writeValueAsString(id));
    }
}
