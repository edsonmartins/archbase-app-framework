package br.com.archbase.ddd.domain.specification.basic;

import br.com.archbase.ddd.domain.specification.ValueBoundArchbaseSpecification;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Objects;

/**
 * Determine se o valor de uma propriedade é menor ou igual o valor especificado.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public class LessThanOrEqualArchbaseSpecification<T> extends ValueBoundArchbaseSpecification<T> {

    public LessThanOrEqualArchbaseSpecification(String property, Object value) {
        super(property, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final boolean isSatisfyingValue(Object candidateValue) {
        return new CompareToBuilder().append(candidateValue, this.getValue()).toComparison() == -1
                || Objects.equals(candidateValue, this.getValue());
    }

}
