package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Null;

/**
 * Válido quando o objeto é nulo <br/>
 */
public class NullValidator implements ConstraintValidator<Null, Object> {

    @Override
    public void initialize(Null annotation) {
        // fazer nada
    }

    public boolean isValid(Object object, ConstraintValidatorContext context) {
        return object == null;
    }
}
