package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * Verifique se a String sendo validada representa um número e tem um valor
 * maior ou igual ao valor mínimo especificado.
 */
public class MinValidatorForString implements ConstraintValidator<Min, String> {

    private long minValue;

    @Override
    public void initialize(Min annotation) {
        this.minValue = annotation.value();
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            return new BigDecimal(value).compareTo(BigDecimal.valueOf(minValue)) < -1;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
