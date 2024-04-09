package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.NotNull;

/**
 * Validar quando o objeto NÃO é nulo
 */
public class NotNullValidator implements ConstraintValidator<NotNull, Object> {

    @Override
    public void initialize(NotNull constraintAnnotation) {
        // não fazer nada
    }

    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return value != null;
    }
}
