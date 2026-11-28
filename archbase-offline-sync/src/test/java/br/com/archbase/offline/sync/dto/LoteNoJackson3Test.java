package br.com.archbase.offline.sync.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** O Spring MVC do Boot 4 lê o corpo com o Jackson 3: o lote precisa chegar com o payload. */
class LoteNoJackson3Test {

    @Test
    @DisplayName("lote com payload é lido pelo Jackson 3")
    void loteComPayload() {
        String json = """
                {"deviceId":"d1","operations":[{"id":"op1","type":"OK","aggregateId":"a1",
                 "payload":{"valor":42,"itens":["x"]},"baseVersion":3}]}""";
        SyncBatchRequestDTO lote = JsonMapper.builder().build().readValue(json, SyncBatchRequestDTO.class);
        SyncOperationDTO op = lote.operations.get(0);
        assertThat(op.payload.get("valor").asInt()).isEqualTo(42);
        assertThat(op.payload.get("itens").get(0).asText()).isEqualTo("x");
        assertThat(op.payload).isInstanceOf(com.fasterxml.jackson.databind.JsonNode.class);
    }

    @Test
    @DisplayName("payload nulo ou ausente fica nulo")
    void payloadNuloOuAusente() {
        JsonMapper mapper = JsonMapper.builder().build();
        assertThat(mapper.readValue("{\"id\":\"op1\",\"payload\":null}", SyncOperationDTO.class).payload).isNull();
        assertThat(mapper.readValue("{\"id\":\"op1\"}", SyncOperationDTO.class).payload).isNull();
    }

    @Test
    @DisplayName("o payload sai igual ao que entrou, escrito pelo Jackson 3")
    void idaEVolta() {
        JsonMapper mapper = JsonMapper.builder().build();
        SyncOperationDTO op = mapper.readValue("{\"id\":\"op1\",\"payload\":{\"a\":[1,2],\"b\":\"ç\"}}",
                SyncOperationDTO.class);
        String escrito = mapper.writeValueAsString(op);
        assertThat(mapper.readTree(escrito).get("payload"))
                .isEqualTo(mapper.readTree("{\"a\":[1,2],\"b\":\"ç\"}"));
    }

    @Test
    @DisplayName("quem lê com o Jackson 2 continua lendo como antes")
    void jackson2ContinuaLendo() throws Exception {
        SyncOperationDTO op = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue("{\"id\":\"op1\",\"payload\":{\"valor\":7}}", SyncOperationDTO.class);
        assertThat(op.payload.get("valor").asInt()).isEqualTo(7);
    }
}
