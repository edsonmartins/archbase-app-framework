package br.com.archbase.shared.kernel.types;

import br.com.archbase.ddd.domain.contracts.ValueObject;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@AllArgsConstructor(staticName = "of")
public class Quantity implements ValueObject {

    private BigDecimal value;

    public Quantity(String val) {
        this.value = new BigDecimal(val);
    }

    public Quantity(double val) {
        this.value = BigDecimal.valueOf(val);
    }

    public Quantity(BigInteger val) {
        this.value = new BigDecimal(val);
    }

    public Quantity(int val) {
        this.value = new BigDecimal(val);
    }

    public Quantity(long val) {
        this.value = new BigDecimal(val);
    }


    public Double doubleValue() {
        if (value == null)
            return null;
        return value.doubleValue();
    }

    /** O valor exato. */
    public BigDecimal getValue() {
        return value;
    }

    /** Soma exata. Até a 3.5.0 passava por {@code double}: 0.1 + 0.2 dava 0.30000000000000004. */
    public Quantity add(Quantity newQuantity) {
        return Quantity.of(value.add(newQuantity.value));
    }

    /** Subtração exata. Até a 3.5.0 passava por {@code double}. */
    public Quantity subtract(Quantity newQuantity) {
        return Quantity.of(value.subtract(newQuantity.value));
    }

    /**
     * Igualdade por valor, ignorando a escala: {@code 1.0} é igual a {@code 1.00}. Até a 3.5.0 não havia
     * {@code equals}: duas quantidades iguais eram diferentes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quantity outra)) {
            return false;
        }
        if (value == null || outra.value == null) {
            return value == outra.value;
        }
        return value.compareTo(outra.value) == 0;
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value == null ? null : value.toPlainString();
    }


}
