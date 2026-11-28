package br.com.archbase.ddd.domain.specification.basic;

import br.com.archbase.ddd.domain.specification.ValueBoundArchbaseSpecification;

/**
 * Afirma se o valor da propriedade não casa com o padrão, como o {@code not like} do SQL ({@code %} e {@code _}).
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public class NotLikeArchbaseSpecification<T> extends ValueBoundArchbaseSpecification<T> {

    /**
     * Construa um novo {@link NotLikeArchbaseSpecification}.
     *
     * @param property expressão da propriedade sendo verificada
     * @param value    the valor que nossa propriedade deve ser igual a
     */
    public NotLikeArchbaseSpecification(String property, String value) {
        super(property, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSatisfyingValue(Object candidateValue) {
        // Como o not like do banco (% e _). Até a 3.5.0 testava se o padrão continha o valor — o
        // inverso — e Like e NotLike davam o mesmo resultado.
        Boolean casa = PadraoLike.casa(getValue(), candidateValue);
        return casa != null && !casa;
    }

}
