package br.com.archbase.ddd.domain.specification.basic;


import br.com.archbase.ddd.domain.specification.ValueBoundArchbaseSpecification;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Objects;

/**
 * Determine se o valor de uma propriedade esta entre dois valores especificados.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public class BetweenArchbaseSpecification<T> extends ValueBoundArchbaseSpecification<T> {

    private final Comparable<Object> lower;
    private final Comparable<Object> upper;

    public BetweenArchbaseSpecification(String property, Object lower, Object upper) {
        super(property, lower);
        this.lower = (Comparable<Object>) lower;
        this.upper = (Comparable<Object>) upper;
    }

    public Comparable<Object> getUpperValue() {
        return upper;
    }

    public Comparable<Object> getLowerValue() {
        return lower;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final boolean isSatisfyingValue(Object candidateValue) {
        return greaterThanOrEqual(candidateValue) && lessThanOrEqual(candidateValue);
    }

    private boolean greaterThanOrEqual(Object candidateValue) {
        return new CompareToBuilder().append(candidateValue, this.getValue()).toComparison() == 1
                || Objects.equals(candidateValue, this.getValue());
    }

    private boolean lessThanOrEqual(Object candidateValue) {
        return new CompareToBuilder().append(candidateValue, this.upper).toComparison() == -1
                || Objects.equals(candidateValue, this.upper);
    }

}
