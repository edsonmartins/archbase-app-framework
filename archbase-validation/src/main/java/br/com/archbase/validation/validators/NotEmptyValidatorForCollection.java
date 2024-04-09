package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.NotEmpty;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;


public class NotEmptyValidatorForCollection implements ConstraintValidator<NotEmpty, Collection<?>> {

    @Override
    public void initialize(NotEmpty constraintAnnotation) {
        // do nothing
    }

    public boolean isValid(Collection<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return !value.isEmpty();
    }
}
