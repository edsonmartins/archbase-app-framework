package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.EAN;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Verifica se uma determinada sequência de caracteres (por exemplo, string) é um código de barras EAN válido.
 */
public class EANValidator implements ConstraintValidator<EAN, CharSequence> {

    private int size;

    @Override
    public void initialize(EAN constraintAnnotation) {
        if (constraintAnnotation.type() == EAN.Type.EAN8) {
            size = 8;
        } else if (constraintAnnotation.type() == EAN.Type.EAN13) {
            size = 13;
        }
    }

    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = value.length();
        return length == size;
    }
}
