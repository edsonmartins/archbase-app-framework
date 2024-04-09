package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;

/**
 * Verifique se a String sendo validada representa um número e tem um valor
 * menor ou igual ao valor máximo especificado.
 */
public class MaxValidatorForString implements ConstraintValidator<Max, String> {

    private long max;

    @Override
    public void initialize(Max annotation) {
        this.max = annotation.value();
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            return new BigDecimal(value).compareTo(BigDecimal.valueOf(max)) < 1;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
