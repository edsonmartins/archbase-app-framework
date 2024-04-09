package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.EqualityArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo EQUAL {@link EqualityConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
public class EqualityConverter implements SpecificationConverter<EqualityArchbaseSpecification<Object>, Object> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate convertToPredicate(EqualityArchbaseSpecification<Object> eq, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.equal(root.get(eq.getProperty()), eq.getValue());
    }

}
