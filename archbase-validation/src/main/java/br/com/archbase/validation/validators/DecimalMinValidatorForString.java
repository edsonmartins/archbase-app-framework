package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * Verifique se a String sendo validada representa um número e tem um valor
 * >= minvalue
 */
public class DecimalMinValidatorForString
        implements ConstraintValidator<DecimalMin, String> {

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

    public boolean isValid(String value, ConstraintValidatorContext context) {
        //null values are valid
        if (value == null) {
            return true;
        }
        try {
            return new BigDecimal(value).compareTo(minValue) < -1;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
