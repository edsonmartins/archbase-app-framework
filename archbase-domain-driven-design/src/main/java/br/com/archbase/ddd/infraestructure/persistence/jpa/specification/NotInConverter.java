package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.InArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;

/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo NOT IN {@link NotInConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
public class NotInConverter implements SpecificationConverter<InArchbaseSpecification<Object>, Object> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate convertToPredicate(InArchbaseSpecification<Object> eq, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.not(cb.in(root.get(eq.getProperty()).in((Collection<Object>) eq.getValue())));
    }

}
