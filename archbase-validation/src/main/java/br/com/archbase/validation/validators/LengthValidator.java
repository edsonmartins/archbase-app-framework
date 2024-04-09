package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.Length;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Verifique se o comprimento da sequência de caracteres está entre mínimo e máximo.
 */
public class LengthValidator implements ConstraintValidator<Length, CharSequence> {

    private int min;
    private int max;

    @Override
    public void initialize(Length parameters) {
        min = parameters.min();
        max = parameters.max();
        validateParameters();
    }

    public boolean isValid(CharSequence value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }
        int length = value.length();
        return length >= min && length <= max;
    }

    private void validateParameters() {
        if (min < 0) {
            throw new IllegalArgumentException("O parâmetro mínimo não pode ser negativo.");
        }
        if (max < 0) {
            throw new IllegalArgumentException("O parâmetro max não pode ser negativo.");
        }
        if (max < min) {
            throw new IllegalArgumentException("O comprimento não pode ser negativo.");
        }
    }
}
