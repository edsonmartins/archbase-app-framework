package br.com.archbase.ddd.domain.specification;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * ArchbaseSpecification que baseia sua avaliação em um valor de propriedade específico. Extensões destA
 * classe são satisfeitas apenas quando o {@link #isSatisfyingValue (Object)} avalia {@code true}.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public abstract class ValueBoundArchbaseSpecification<T> implements ComposableArchbaseSpecification<T> {
    private final String property;
    private final Object value;

    /**
     * Construct a new {@link ValueBoundArchbaseSpecification}.
     *
     * @param property expression of the property being checked
     * @param value    the value that our property should conform to
     */
    protected ValueBoundArchbaseSpecification(String property, Object value) {
        super();
        this.property = property;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isSatisfiedBy(T candidate) {
        Field field = ReflectionUtils.findField(candidate.getClass(), property);
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            Object candidateValue = ReflectionUtils.getField(field, candidate);
            if (candidateValue != null) {
                return isSatisfyingValue(candidateValue);
            }
        }
        return false;
    }

    /**
     * See if a property value satisfies all the requirements expressed in this specification.
     *
     * @param candidateValue the property value being verified
     * @return {@code true} if the requirements are satisfied, otherwise {@code false}
     */
    protected abstract boolean isSatisfyingValue(Object candidateValue);

    /**
     * @return
     */
    public String getProperty() {
        return property;
    }

    /**
     * @return
     */
    public Object getValue() {
        return value;
    }

}
