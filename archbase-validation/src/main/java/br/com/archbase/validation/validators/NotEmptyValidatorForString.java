package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.NotEmpty;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class NotEmptyValidatorForString implements ConstraintValidator<NotEmpty, String> {

    @Override
    public void initialize(NotEmpty constraintAnnotation) {
        // fazer nada
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return !value.isEmpty();
    }
}
