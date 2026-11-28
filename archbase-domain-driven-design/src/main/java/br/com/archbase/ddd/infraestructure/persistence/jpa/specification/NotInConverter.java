package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.basic.NotInArchbaseSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collection;

/**
 * {@code not in}. Até a 3.5.0 declarava {@code InArchbaseSpecification} e tomava o lugar do conversor de {@code in}.
 */
public class NotInConverter implements SpecificationConverter<NotInArchbaseSpecification<Object>, Object> {

    @Override
    @SuppressWarnings("unchecked")
    public Predicate convertToPredicate(NotInArchbaseSpecification<Object> s, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.not(root.get(s.getProperty()).in((Collection<Object>) s.getValue()));
    }
}
