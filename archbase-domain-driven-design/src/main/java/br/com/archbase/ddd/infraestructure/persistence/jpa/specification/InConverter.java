package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.basic.InArchbaseSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collection;

/**
 * {@code in}. Até a 3.5.0 aninhava o predicado dentro de outro {@code in} vazio.
 */
public class InConverter implements SpecificationConverter<InArchbaseSpecification<Object>, Object> {

    @Override
    @SuppressWarnings("unchecked")
    public Predicate convertToPredicate(InArchbaseSpecification<Object> s, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return root.get(s.getProperty()).in((Collection<Object>) s.getValue());
    }
}
