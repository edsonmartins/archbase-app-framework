package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * Verifique se o número que está sendo validado é menor ou igual ao máximo
 * valor especificado.
 */
public class MaxValidatorForNumber implements ConstraintValidator<Max, Number> {

    private long max;

    @Override
    public void initialize(Max annotation) {
        this.max = annotation.value();
    }

    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(BigDecimal.valueOf(max)) < 1;
        } else if (value instanceof BigInteger) {
            return ((BigInteger) value).compareTo(BigInteger.valueOf(max)) < 1;
        } else {
            return value.longValue() <= max;
        }
    }
}
