package br.com.archbase.ddd.domain.specification.basic;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.ValueBoundArchbaseSpecification;
import org.apache.commons.lang3.builder.CompareToBuilder;

/**
 * Abstração de CompareTo a ser usado com {@link ArchbaseSpecification}
 *
 * @param <T> Tipo de classe
 * @author edsonmartins
 */
public abstract class CompareToArchbaseSpecification<T> extends ValueBoundArchbaseSpecification<T> {
    private final int expectedComparison;

    protected CompareToArchbaseSpecification(String property, Object value, int expectedComparison) {
        super(property, value);
        this.expectedComparison = expectedComparison;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSatisfyingValue(Object candidateValue) {
        return new CompareToBuilder().append(candidateValue, this.getValue()).toComparison() == expectedComparison;
    }
}
