package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.NotArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo NOT {@link NotConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
public class NotConverter implements SpecificationConverter<NotArchbaseSpecification<Object>, Object> {
    private final SpecificationTranslator translator;

    public NotConverter(SpecificationTranslator translator) {
        super();
        this.translator = translator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate convertToPredicate(NotArchbaseSpecification<Object> not, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.not(translator.translateToPredicate(not.getProposition(), root, cq, cb));
    }

}
