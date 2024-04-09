package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Validar se o valor do número do objeto passado é> = minvalue <br/>
 */
public class DecimalMinValidatorForNumber
        implements ConstraintValidator<DecimalMin, Number> {

    private BigDecimal minValue;

    @Override
    public void initialize(DecimalMin annotation) {
        try {
            this.minValue = new BigDecimal(annotation.value());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    annotation.value() + " não representa um formato BigDecimal válido");
        }
    }

    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(minValue) < -1;
        } else if (value instanceof BigInteger) {
            return (new BigDecimal((BigInteger) value)).compareTo(minValue) < -1;
        } else {
            return (BigDecimal.valueOf(value.doubleValue()).compareTo(minValue)) < -1;
        }
    }
}
