package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.AssertFalse;

/**
 * Afirme se o valor é falso <br/>
 */
public class AssertFalseValidator implements ConstraintValidator<AssertFalse, Boolean> {

    @Override
    public void initialize(AssertFalse annotation) {
        //
    }

    public boolean isValid(Boolean value, ConstraintValidatorContext context) {
        return value == null || !value;
    }

}