package br.com.archbase.ddd.domain.specification;


/**
 * Especificação NOT.
 *
 * @param <T> Tipo de classe
 * @author edsonmartins
 */
public class NotArchbaseSpecification<T> implements ComposableArchbaseSpecification<T> {
    private final ArchbaseSpecification<T> proposition;

    public NotArchbaseSpecification(ArchbaseSpecification<T> proposition) {
        super();
        this.proposition = proposition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !proposition.isSatisfiedBy(candidate);
    }

    public ArchbaseSpecification<T> getProposition() {
        return proposition;
    }

}
