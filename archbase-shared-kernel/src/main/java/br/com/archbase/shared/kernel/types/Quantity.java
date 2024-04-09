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

    public Quantity add(Quantity newQuantity) {
        return new Quantity(value.add(newQuantity.value).doubleValue());
    }

    public Quantity subtract(Quantity newQuantity) {
        return new Quantity(value.subtract(newQuantity.value).doubleValue());
    }


}
