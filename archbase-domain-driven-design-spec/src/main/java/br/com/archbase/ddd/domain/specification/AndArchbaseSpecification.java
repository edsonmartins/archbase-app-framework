package br.com.archbase.ddd.domain.specification;

/**
 * Especificação AND.
 *
 * @param <T> Tipo de classe
 * @author edsonmartins
 */
public class AndArchbaseSpecification<T> implements ComposableArchbaseSpecification<T> {
    private final ArchbaseSpecification<T> lhs;
    private final ArchbaseSpecification<T> rhs;

    public AndArchbaseSpecification(ArchbaseSpecification<T> lhs, ArchbaseSpecification<T> rhs) {
        super();
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSatisfiedBy(T candidate) {
        return lhs.isSatisfiedBy(candidate) && rhs.isSatisfiedBy(candidate);
    }

    public ArchbaseSpecification<T> getLhs() {
        return lhs;
    }

    public ArchbaseSpecification<T> getRhs() {
        return rhs;
    }

}
