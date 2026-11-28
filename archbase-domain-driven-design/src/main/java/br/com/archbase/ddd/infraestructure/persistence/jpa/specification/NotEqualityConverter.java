package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.basic.NotEqualityArchbaseSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Diferença. Até a 3.5.0 não havia conversor: a specification era recusada.
 */
public class NotEqualityConverter implements SpecificationConverter<NotEqualityArchbaseSpecification<Object>, Object> {

    @Override
    public Predicate convertToPredicate(NotEqualityArchbaseSpecification<Object> s, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.notEqual(root.get(s.getProperty()), s.getValue());
    }
}
