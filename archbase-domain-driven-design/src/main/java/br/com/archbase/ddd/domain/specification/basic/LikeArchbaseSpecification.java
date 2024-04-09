package br.com.archbase.ddd.domain.specification.basic;

import br.com.archbase.ddd.domain.specification.ValueBoundArchbaseSpecification;

/**
 * Afirma se os candidatos estão contidos no valor de propriedade.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public class LikeArchbaseSpecification<T> extends ValueBoundArchbaseSpecification<T> {

    /**
     * Construa um novo {@link LikeArchbaseSpecification}.
     *
     * @param property expressão da propriedade sendo verificada
     * @param value    the valor que nossa propriedade deve ser igual a
     */
    public LikeArchbaseSpecification(String property, String value) {
        super(property, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSatisfyingValue(Object candidateValue) {
        return ((String) getValue()).contains(String.valueOf(candidateValue));
    }

}
