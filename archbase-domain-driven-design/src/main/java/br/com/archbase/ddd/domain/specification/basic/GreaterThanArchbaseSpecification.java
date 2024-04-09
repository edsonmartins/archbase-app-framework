package br.com.archbase.ddd.domain.specification.basic;

/**
 * Determine se o valor de uma propriedade é maior que o valor especificado.
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edson martins
 */
public class GreaterThanArchbaseSpecification<T> extends CompareToArchbaseSpecification<T> {
    private static final int GREATER_THAN_COMPARISON = 1;

    /**
     * Construa um novo {@link GreaterThanArchbaseSpecification}.
     *
     * @param property determina qual propriedade deve ser verificada
     * @param value    os candidatos só são combinados quando o valor de sua propriedade está acima desse valor
     */
    public GreaterThanArchbaseSpecification(String property, Object value) {
        super(property, value, GREATER_THAN_COMPARISON);
    }

}
