package br.com.archbase.ddd.domain.specification;

/**
 * ArchbaseSpecification que pode combinar sua lógica com especificações adicionais.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public interface ComposableArchbaseSpecification<T> extends ArchbaseSpecification<T> {

    /**
     * AND
     *
     * @param rhs
     * @return
     */
    public default AndArchbaseSpecification<T> and(ArchbaseSpecification<T> rhs) {
        return new AndArchbaseSpecification<>(this, rhs);
    }

    /**
     * OR
     *
     * @param rhs
     * @return
     */
    public default OrArchbaseSpecification<T> or(ArchbaseSpecification<T> rhs) {
        return new OrArchbaseSpecification<>(this, rhs);
    }

    /**
     * NOT
     *
     * @return
     */
    public default NotArchbaseSpecification<T> not() {
        return new NotArchbaseSpecification<>(this);
    }

}
