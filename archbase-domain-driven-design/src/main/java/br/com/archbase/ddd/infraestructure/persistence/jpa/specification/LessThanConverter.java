package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.LessThanArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo LESS THAN {@link LessThanConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
public class LessThanConverter implements SpecificationConverter<LessThanArchbaseSpecification<Object>, Object> {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Predicate convertToPredicate(LessThanArchbaseSpecification<Object> lt, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.lessThan(root.get(lt.getProperty()), lt.getValue().toString());
    }

}
