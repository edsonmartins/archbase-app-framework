package br.com.archbase.ddd.domain.specification.basic;

import br.com.archbase.ddd.domain.specification.ValueBoundArchbaseSpecification;

import java.util.Collection;

/**
 * Afirma se os candidatos não estão contidos numa coleção de valores.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public class NotInArchbaseSpecification<T> extends ValueBoundArchbaseSpecification<T> {

    /**
     * Construa um novo {@link NotInArchbaseSpecification}.
     *
     * @param property expressão da propriedade sendo verificada
     * @param value    the valor que nossa propriedade deve ser igual a
     */
    public NotInArchbaseSpecification(String property, Collection<Object> value) {
        super(property, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSatisfyingValue(Object candidateValue) {
        return !((Collection<Object>) getValue()).contains(candidateValue);
    }

}
