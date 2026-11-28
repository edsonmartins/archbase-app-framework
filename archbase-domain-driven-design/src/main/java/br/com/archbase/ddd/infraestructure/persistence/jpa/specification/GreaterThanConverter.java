package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.basic.GreaterThanArchbaseSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import jakarta.persistence.criteria.Expression;

/**
 * {@code >}. Compara com o valor tal como veio (até a 3.5.0, convertido em texto: num campo numérico, comparava texto com número).
 */
public class GreaterThanConverter implements SpecificationConverter<GreaterThanArchbaseSpecification<Object>, Object> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Predicate convertToPredicate(GreaterThanArchbaseSpecification<Object> s, Root<Object> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        Expression<Comparable> campo = root.get(s.getProperty());
        return cb.greaterThan(campo, (Comparable) s.getValue());
    }
}
