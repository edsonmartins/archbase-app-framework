package br.com.archbase.ddd.domain.specification.basic;

/**
 * Determine se o valor de uma propriedade é menor que o valor especificado.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public class LessThanArchbaseSpecification<T> extends CompareToArchbaseSpecification<T> {
    private static final int LESS_THAN_COMPARISON = -1;

    /**
     * Construa um novo{@link LessThanArchbaseSpecification}.
     *
     * @param property determina qual propriedade deve ser verificada
     * @param value    os candidatos só são combinados quando o valor de sua propriedade é menor que esse valor
     */
    public LessThanArchbaseSpecification(String property, Object value) {
        super(property, value, LESS_THAN_COMPARISON);
    }

}
