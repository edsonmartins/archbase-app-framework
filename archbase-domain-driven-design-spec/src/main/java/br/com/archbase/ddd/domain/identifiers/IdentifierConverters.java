package br.com.archbase.ddd.domain.identifiers;

import br.com.archbase.ddd.domain.contracts.Identifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Conversores Spring entre identificadores tipados de valor único (ver {@link Identifiers}) e qualquer tipo que o
 * {@link ConversionService} saiba converter para o valor — por exemplo {@code String -> OrderId(UUID)} em
 * {@code @PathVariable}/{@code @RequestParam}, e {@code OrderId -> String} na volta.
 * <p>
 * Não são registrados automaticamente. Para ligar no Spring MVC:
 * <pre>{@code
 * @Configuration
 * class WebConfig implements WebMvcConfigurer {
 *     @Override
 *     public void addFormatters(FormatterRegistry registry) {
 *         IdentifierConverters.register(registry);
 *     }
 * }
 * }</pre>
 *
 * @since 3.4.6
 */
public final class IdentifierConverters {

    private IdentifierConverters() {
    }

    /**
     * Registra os conversores. Se o registry também for um {@link ConversionService} (caso do Spring MVC), ele é
     * usado para a conversão do valor; senão, o {@link DefaultConversionService} compartilhado.
     */
    public static void register(ConverterRegistry registry) {
        ConversionService delegate = registry instanceof ConversionService service
                ? service : DefaultConversionService.getSharedInstance();
        register(registry, () -> delegate);
    }

    /** Registra os conversores usando o {@link ConversionService} informado para a conversão do valor. */
    public static void register(ConverterRegistry registry, Supplier<ConversionService> delegate) {
        registry.addConverter(new ToIdentifier(delegate));
        registry.addConverter(new FromIdentifier(delegate));
    }

    /** Valor (ou algo conversível para ele) -> identificador. */
    static final class ToIdentifier implements ConditionalGenericConverter {

        private final Supplier<ConversionService> delegate;

        ToIdentifier(Supplier<ConversionService> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Set.of(new ConvertiblePair(Object.class, Identifier.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            Class<?> target = targetType.getType();
            if (!Identifiers.isSingleValue(target) || target.isAssignableFrom(sourceType.getType())) {
                return false;
            }
            Class<?> valueType = Identifiers.valueType(target.asSubclass(Identifier.class));
            return valueType.isAssignableFrom(sourceType.getType())
                    || delegate.get().canConvert(sourceType, TypeDescriptor.valueOf(valueType));
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (source == null) {
                return null;
            }
            Class<? extends Identifier> target = targetType.getType().asSubclass(Identifier.class);
            Class<?> valueType = Identifiers.valueType(target);
            Object value = valueType.isInstance(source)
                    ? source : delegate.get().convert(source, sourceType, TypeDescriptor.valueOf(valueType));
            return Identifiers.of(target, value);
        }
    }

    /** Identificador -> valor (ou algo para o qual o valor seja conversível). */
    static final class FromIdentifier implements ConditionalGenericConverter {

        private final Supplier<ConversionService> delegate;

        FromIdentifier(Supplier<ConversionService> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Set.of(new ConvertiblePair(Identifier.class, Object.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            Class<?> source = sourceType.getType();
            if (!Identifiers.isSingleValue(source) || targetType.getType().isAssignableFrom(source)) {
                return false;
            }
            Class<?> valueType = Identifiers.valueType(source.asSubclass(Identifier.class));
            return targetType.getType().isAssignableFrom(valueType)
                    || delegate.get().canConvert(TypeDescriptor.valueOf(valueType), targetType);
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            if (source == null) {
                return null;
            }
            Object value = Identifiers.valueOf((Identifier) source);
            if (targetType.getType().isInstance(value)) {
                return value;
            }
            return delegate.get().convert(value, TypeDescriptor.forObject(value), targetType);
        }
    }
}
