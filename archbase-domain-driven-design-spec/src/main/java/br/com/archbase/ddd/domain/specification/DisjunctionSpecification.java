package br.com.archbase.ddd.domain.specification;

public class DisjunctionSpecification<T> implements ArchbaseSpecification<T> {
    private ArchbaseSpecification<T>[] disjunction;

    public DisjunctionSpecification(ArchbaseSpecification<T>... disjunction) {
        this.disjunction = disjunction;
    }

    public boolean isSatisfiedBy(T candidate) {
        for (ArchbaseSpecification<T> spec : disjunction) {
            if (spec.isSatisfiedBy(candidate))
                return true;
        }

        return false;
    }
}
