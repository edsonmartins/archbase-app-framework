package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;


public class NotEmptyValidatorForMap implements ConstraintValidator<NotEmpty, Map<?, ?>> {

    @Autowired
    @Override
    public void initialize(NotEmpty constraintAnnotation) {
        // fazer nada
    }

    public boolean isValid(Map<?, ?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return !value.isEmpty();
    }
}
