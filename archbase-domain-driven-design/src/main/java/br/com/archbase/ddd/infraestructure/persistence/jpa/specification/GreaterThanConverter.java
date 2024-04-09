package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.GreaterThanArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo GREATER THAN {@link GreaterThanOrEqualConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
public class GreaterThanConverter implements SpecificationConverter<GreaterThanArchbaseSpecification<Object>, Object> {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Predicate convertToPredicate(GreaterThanArchbaseSpecification<Object> gt, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.greaterThan(root.get(gt.getProperty()), gt.getValue().toString());
    }

}
