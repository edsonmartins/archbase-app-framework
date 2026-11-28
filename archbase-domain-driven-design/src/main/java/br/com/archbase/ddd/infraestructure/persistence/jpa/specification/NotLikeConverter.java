package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.basic.NotLikeArchbaseSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * {@code not like}. Até a 3.5.0 declarava {@code EqualityArchbaseSpecification}: como era registrado por último, toda igualdade virava {@code not like}.
 */
public class NotLikeConverter implements SpecificationConverter<NotLikeArchbaseSpecification<Object>, Object> {

    @Override
    public Predicate convertToPredicate(NotLikeArchbaseSpecification<Object> s, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.notLike(root.<String>get(s.getProperty()), String.valueOf(s.getValue()));
    }
}
