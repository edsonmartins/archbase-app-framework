package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.DecimalMax;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Validar se o valor do número informado é <= maxvalue <br />
 */

public class DecimalMaxValidatorForNumber
        implements ConstraintValidator<DecimalMax, Number> {

    private BigDecimal maxValue;

    @Override
    public void initialize(DecimalMax annotation) {
        try {
            this.maxValue = new BigDecimal(annotation.value());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    annotation.value() + " não representa um formato BigDecimal válido");
        }
    }

    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(maxValue) < 1;
        } else if (value instanceof BigInteger) {
            return (new BigDecimal((BigInteger) value)).compareTo(maxValue) < 1;
        } else {
            return (BigDecimal.valueOf(value.doubleValue()).compareTo(maxValue)) < 1;
        }
    }
}
