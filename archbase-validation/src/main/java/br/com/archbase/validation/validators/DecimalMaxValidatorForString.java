package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.DecimalMax;
import java.math.BigDecimal;

/**
 * Verifique se a String sendo validada representa um número e tem um valor
 * <= maxvalue
 */
public class DecimalMaxValidatorForString
        implements ConstraintValidator<DecimalMax, String> {

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

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            return new BigDecimal(value).compareTo(maxValue) < 1;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
