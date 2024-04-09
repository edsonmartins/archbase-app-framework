package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.BetweenArchbaseSpecification;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.criteria.*;


/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} do tipo BETWEEN {@link BetweenConverter} em {@link Predicate}.
 *
 * @author edsonmartins
 */
@SuppressWarnings("rawtypes")
public class BetweenConverter implements SpecificationConverter<BetweenArchbaseSpecification<Object>, Object> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate convertToPredicate(BetweenArchbaseSpecification<Object> bt, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        return cb.between(root.get(bt.getProperty()), bt.getLowerValue(), bt.getUpperValue());
    }

    public String getProperty(String property) {
        if (property.contains(".")) {
            return StringUtils.split(property, ".")[1];
        }
        return property;
    }

    public From getRoot(String property, Root<Object> root) {
        if (property.contains(".")) {
            String joinProperty = StringUtils.split(property, ".")[0];
            return root.join(joinProperty, JoinType.LEFT);
        }
        return root;
    }

}
