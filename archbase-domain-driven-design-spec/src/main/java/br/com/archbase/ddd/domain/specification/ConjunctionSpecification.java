package br.com.archbase.ddd.domain.specification;

import java.util.List;

public class ConjunctionSpecification<T> implements ArchbaseSpecification<T> {

    private List<ArchbaseSpecification<T>> list;

    public ConjunctionSpecification(List<ArchbaseSpecification<T>> list) {
        this.list = list;
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        for (ArchbaseSpecification<T> spec : list) {
            if (!spec.isSatisfiedBy(candidate))
                return false;
        }
        return true;
    }
}
