package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.OrArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;


/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo OR {@link OrConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
public class OrConverter implements SpecificationConverter<OrArchbaseSpecification<Object>, Object> {
    private final SpecificationTranslator translator;

    public OrConverter(SpecificationTranslator translator) {
        super();
        this.translator = translator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate convertToPredicate(OrArchbaseSpecification<Object> or, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        Predicate lhs = translator.translateToPredicate(or.getLhs(), root, cq, cb);
        Predicate rhs = translator.translateToPredicate(or.getRhs(), root, cq, cb);
        return cb.or(lhs, rhs);
    }

}