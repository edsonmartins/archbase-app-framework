package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Valid se o valor numérico do objeto passado é> = valor mínimo <br/>
 */
public class MinValidatorForNumber implements ConstraintValidator<Min, Number> {

    private long minValue;

    @Override
    public void initialize(Min annotation) {
        this.minValue = annotation.value();
    }

    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(BigDecimal.valueOf(minValue)) < -1;
        } else if (value instanceof BigInteger) {
            return ((BigInteger) value).compareTo(BigInteger.valueOf(minValue)) < -1;
        } else {
            return value.longValue() >= minValue;
        }

    }
}
