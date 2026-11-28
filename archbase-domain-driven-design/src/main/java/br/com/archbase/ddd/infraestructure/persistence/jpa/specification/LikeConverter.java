package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.basic.LikeArchbaseSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * {@code like}. Até a 3.5.0 declarava {@code EqualityArchbaseSpecification} e tomava o lugar do conversor de igualdade.
 */
public class LikeConverter implements SpecificationConverter<LikeArchbaseSpecification<Object>, Object> {

    @Override
    public Predicate convertToPredicate(LikeArchbaseSpecification<Object> s, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.like(root.<String>get(s.getProperty()), String.valueOf(s.getValue()));
    }
}
