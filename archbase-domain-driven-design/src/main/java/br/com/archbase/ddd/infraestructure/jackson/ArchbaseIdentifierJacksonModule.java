package br.com.archbase.ddd.infraestructure.jackson;

import br.com.archbase.ddd.domain.contracts.Identifier;
import br.com.archbase.ddd.domain.identifiers.Identifiers;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.Serializers;

/**
 * Módulo Jackson 3 que serializa um identificador tipado de valor único (ver {@link Identifiers}) como o valor que
 * ele embrulha, e o lê de volta do mesmo jeito:
 * <pre>{@code
 * record OrderId(UUID value) implements Identifier { }
 *
 * // sem o módulo: {"id": {"value": "3f1c…"}}
 * // com o módulo: {"id": "3f1c…"}
 * }</pre>
 * Só afeta records de um componente que implementam {@link Identifier}; os demais identificadores, inclusive
 * {@link br.com.archbase.ddd.domain.base.ArchbaseIdentifier}, mantêm o formato atual.
 * <p>
 * Não é registrado automaticamente, porque muda o JSON desses tipos. Para ligar numa aplicação Spring Boot, exponha o
 * módulo como bean — o Boot o adiciona ao {@code JsonMapper}:
 * <pre>{@code
 * @Bean
 * JacksonModule archbaseIdentifierModule() {
 *     return new ArchbaseIdentifierJacksonModule();
 * }
 * }</pre>
 * Identificadores usados como <em>chave</em> de {@code Map} não são cobertos.
 *
 * @since 3.4.6
 */
public class ArchbaseIdentifierJacksonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public ArchbaseIdentifierJacksonModule() {
        super("ArchbaseIdentifierModule");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addSerializers(new IdentifierSerializers());
        context.addDeserializers(new IdentifierDeserializers());
    }

    private static final class IdentifierSerializers extends Serializers.Base {

        @Override
        public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type,
                                                 BeanDescription.Supplier beanDescRef,
                                                 JsonFormat.Value formatOverrides) {
            return Identifiers.isSingleValue(type.getRawClass()) ? IdentifierSerializer.INSTANCE : null;
        }
    }

    private static final class IdentifierDeserializers extends Deserializers.Base {

        @Override
        public ValueDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config,
                                                         BeanDescription.Supplier beanDescRef) {
            Class<?> raw = type.getRawClass();
            return Identifiers.isSingleValue(raw)
                    ? new IdentifierDeserializer(raw.asSubclass(Identifier.class)) : null;
        }

        @Override
        public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
            return Identifiers.isSingleValue(valueType);
        }
    }

    private static final class IdentifierSerializer extends ValueSerializer<Identifier> {

        static final IdentifierSerializer INSTANCE = new IdentifierSerializer();

        @Override
        public void serialize(Identifier value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            ctxt.writeValue(gen, Identifiers.valueOf(value));
        }
    }

    private static final class IdentifierDeserializer extends ValueDeserializer<Identifier> {

        private final Class<? extends Identifier> type;
        private final Class<?> valueType;

        IdentifierDeserializer(Class<? extends Identifier> type) {
            this.type = type;
            this.valueType = Identifiers.valueType(type);
        }

        @Override
        public Identifier deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            return Identifiers.of(type, ctxt.readValue(p, valueType));
        }
    }
}
