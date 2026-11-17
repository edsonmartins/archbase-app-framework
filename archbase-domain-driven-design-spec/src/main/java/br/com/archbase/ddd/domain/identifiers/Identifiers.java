package br.com.archbase.ddd.domain.identifiers;

import br.com.archbase.ddd.domain.contracts.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suporte a <b>identificadores tipados de valor único</b>: um {@code record} com exatamente um componente que
 * implementa {@link Identifier}.
 * <pre>{@code
 * public record OrderId(UUID value) implements Identifier { }
 * }</pre>
 * É a base dos conversores de JPA ({@link AbstractIdentifierAttributeConverter}), Spring
 * ({@link IdentifierConverters}) e Jackson ({@code ArchbaseIdentifierJacksonModule}), que tratam o identificador como o
 * valor que ele embrulha. Qualquer outro {@link Identifier} — inclusive
 * {@link br.com.archbase.ddd.domain.base.ArchbaseIdentifier} — é ignorado por eles, para não mudar formato nenhum que
 * já exista.
 *
 * @since 3.4.6
 */
public final class Identifiers {

    private static final Map<Class<?>, Optional<Shape>> SHAPES = new ConcurrentHashMap<>();

    private Identifiers() {
    }

    /** Se o tipo é um identificador tipado de valor único. */
    public static boolean isSingleValue(Class<?> type) {
        return shapeOf(type).isPresent();
    }

    /** O tipo do valor embrulhado. */
    public static Class<?> valueType(Class<? extends Identifier> type) {
        return requireShape(type).component.getType();
    }

    /** O valor embrulhado pelo identificador; {@code null} para identificador {@code null}. */
    public static Object valueOf(Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        Shape shape = requireShape(identifier.getClass());
        try {
            return shape.component.getAccessor().invoke(identifier);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Não foi possível ler o valor de " + identifier.getClass().getName(), e);
        }
    }

    /**
     * Cria o identificador a partir do valor; {@code null} para valor {@code null}. O valor precisa ser do tipo do
     * componente (sem conversão — para isso, use {@link IdentifierConverters}).
     */
    public static <ID extends Identifier> ID of(Class<ID> type, Object value) {
        if (value == null) {
            return null;
        }
        Shape shape = requireShape(type);
        try {
            return type.cast(shape.constructor.newInstance(value));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalArgumentException("Não foi possível criar " + type.getName() + " de " + value, cause);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Não foi possível criar " + type.getName() + " de " + value, e);
        }
    }

    private static Shape requireShape(Class<?> type) {
        return shapeOf(type).orElseThrow(() -> new IllegalArgumentException(type.getName()
                + " não é um identificador tipado de valor único (record com um componente que implementa Identifier)"));
    }

    private static Optional<Shape> shapeOf(Class<?> type) {
        if (type == null) {
            return Optional.empty();
        }
        return SHAPES.computeIfAbsent(type, Identifiers::inspect);
    }

    private static Optional<Shape> inspect(Class<?> type) {
        if (!type.isRecord() || !Identifier.class.isAssignableFrom(type)) {
            return Optional.empty();
        }
        RecordComponent[] components = type.getRecordComponents();
        if (components.length != 1) {
            return Optional.empty();
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(components[0].getType());
            constructor.setAccessible(true);
            components[0].getAccessor().setAccessible(true);
            return Optional.of(new Shape(components[0], constructor));
        } catch (NoSuchMethodException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private record Shape(RecordComponent component, Constructor<?> constructor) {
    }
}
