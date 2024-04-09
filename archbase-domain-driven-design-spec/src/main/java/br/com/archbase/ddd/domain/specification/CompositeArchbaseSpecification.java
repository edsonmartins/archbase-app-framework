package br.com.archbase.ddd.domain.specification;

/**
 * Especificação composta.
 *
 * @param <T> Tipo de classe
 * @author edsonmartins
 */
public abstract class CompositeArchbaseSpecification<T> implements AbstractArchbaseSpecification<T> {
    private final ArchbaseSpecification<T> lhs;
    private final ArchbaseSpecification<T> rhs;

    protected CompositeArchbaseSpecification(ArchbaseSpecification<T> lhs, ArchbaseSpecification<T> rhs) {
        super();
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public ArchbaseSpecification<T> getLhs() {
        return lhs;
    }

    public ArchbaseSpecification<T> getRhs() {
        return rhs;
    }

}
