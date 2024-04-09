package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.AndArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo AND {@link AndConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
public class AndConverter implements SpecificationConverter<AndArchbaseSpecification<Object>, Object> {
    private final SpecificationTranslator translator;

    public AndConverter(SpecificationTranslator translator) {
        super();
        this.translator = translator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate convertToPredicate(AndArchbaseSpecification<Object> and, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        Predicate lhs = translator.translateToPredicate(and.getLhs(), root, cq, cb);
        Predicate rhs = translator.translateToPredicate(and.getRhs(), root, cq, cb);
        return cb.and(lhs, rhs);
    }

}