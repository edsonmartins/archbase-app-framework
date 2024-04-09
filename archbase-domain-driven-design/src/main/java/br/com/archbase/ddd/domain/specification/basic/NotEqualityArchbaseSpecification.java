package br.com.archbase.ddd.domain.specification.basic;

import br.com.archbase.ddd.domain.specification.ValueBoundArchbaseSpecification;

import java.util.Objects;

/**
 * Afirma se os candidatos são diferentes. Para que esta especificação seja satisfeita,
 * o valor da propriedade do candidato deve ser semanticamente diferente ao valor esperado da propriedade. Objetos são
 * considerado semanticamente diferentes, quando {@link #equals(Object)} avaliar {@code false}.
 * <p>
 * <code>
 * ArchbaseSpecification&lt;Person&gt; namedAntonio = new EqualityArchbaseSpecification&lt;Person&gt;("name","antonio"); <br/>
 * namedHenk.isSatisfiedBy(new Person().named("edson")); // Avalia verdadeiro <br/>
 * namedHenk.isSatisfiedBy(new Person().named("leonardo")); // Avalia falso <br/>
 * <code>
 *
 * @param <T> tipo de candidatos sendo verificados
 * @author edsonmartins
 */
public class NotEqualityArchbaseSpecification<T> extends ValueBoundArchbaseSpecification<T> {

    /**
     * Construa um novo {@link NotEqualityArchbaseSpecification}.
     *
     * @param property expressão da propriedade sendo verificada
     * @param value    the valor que nossa propriedade deve ser diferente de
     */
    public NotEqualityArchbaseSpecification(String property, Object value) {
        super(property, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isSatisfyingValue(Object candidateValue) {
        return !Objects.equals(candidateValue, getValue());
    }

}
