package br.com.archbase.ddd.domain.identifiers;

import br.com.archbase.ddd.domain.contracts.Identifier;
import jakarta.persistence.AttributeConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Base para o conversor JPA de um identificador tipado de valor único (ver {@link Identifiers}). O JPA exige uma
 * classe concreta por tipo, então basta declarar:
 * <pre>{@code
 * @Converter(autoApply = true)
 * public class OrderIdConverter extends AbstractIdentifierAttributeConverter<OrderId, UUID> { }
 * }</pre>
 * Com {@code autoApply = true}, todo atributo {@code OrderId} vira uma coluna do tipo do valor.
 *
 * @param <ID> o identificador
 * @param <V>  o tipo do valor gravado na coluna
 * @since 3.4.6
 */
public abstract class AbstractIdentifierAttributeConverter<ID extends Identifier, V> implements AttributeConverter<ID, V> {

    private final Class<ID> identifierType;

    protected AbstractIdentifierAttributeConverter() {
        this.identifierType = resolveIdentifierType();
        if (!Identifiers.isSingleValue(identifierType)) {
            throw new IllegalStateException(identifierType.getName()
                    + " não é um identificador tipado de valor único (record com um componente que implementa Identifier)");
        }
    }

    /** Para subclasses que não conseguem declarar o tipo pelo genérico. */
    protected AbstractIdentifierAttributeConverter(Class<ID> identifierType) {
        this.identifierType = identifierType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V convertToDatabaseColumn(ID attribute) {
        return (V) Identifiers.valueOf(attribute);
    }

    @Override
    public ID convertToEntityAttribute(V dbData) {
        return Identifiers.of(identifierType, dbData);
    }

    @SuppressWarnings("unchecked")
    private Class<ID> resolveIdentifierType() {
        Class<?> type = getClass();
        while (type != null && type.getSuperclass() != AbstractIdentifierAttributeConverter.class) {
            type = type.getSuperclass();
        }
        if (type != null) {
            Type generic = type.getGenericSuperclass();
            if (generic instanceof ParameterizedType parameterized
                    && parameterized.getActualTypeArguments()[0] instanceof Class<?> resolved) {
                return (Class<ID>) resolved;
            }
        }
        throw new IllegalStateException("Declare o tipo do identificador em " + getClass().getName()
                + " (extends AbstractIdentifierAttributeConverter<MeuId, Valor>) ou use o construtor com Class");
    }
}
