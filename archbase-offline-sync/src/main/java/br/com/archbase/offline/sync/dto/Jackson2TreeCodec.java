package br.com.archbase.offline.sync.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Lê e escreve, pelo Jackson 3, um campo do tipo {@link JsonNode} do Jackson 2.
 *
 * <p>O {@code payload} de {@link SyncOperationDTO} é um {@code JsonNode} do Jackson 2, e é assim que os handlers da
 * aplicação o recebem. O Spring MVC do Boot 4 lê o corpo com o Jackson 3, que não sabe construir esse tipo: sem este
 * conversor, todo lote com {@code payload} era recusado antes de chegar ao handler. A árvore lida pelo Jackson 3 é
 * repassada ao Jackson 2 como texto JSON — mesmo conteúdo, mesmo tipo de antes para o handler.
 */
final class Jackson2TreeCodec {

    private static final ObjectMapper JACKSON2 = new ObjectMapper();

    private Jackson2TreeCodec() {
    }

    static final class Deserializer extends ValueDeserializer<JsonNode> {
        @Override
        public JsonNode deserialize(JsonParser parser, DeserializationContext ctxt) {
            tools.jackson.databind.JsonNode tree = ctxt.readTree(parser);
            try {
                return JACKSON2.readTree(tree.toString());
            } catch (JsonProcessingException e) {
                throw MismatchedInputException.from(parser, JsonNode.class, e.getOriginalMessage());
            }
        }
    }

    static final class Serializer extends ValueSerializer<JsonNode> {
        @Override
        public void serialize(JsonNode value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeRawValue(value.toString());
        }
    }
}
