package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.AssertTrue;

/**
 * Afirme se o valor é verdadeiro <br/>
 */
public class AssertTrueValidator implements ConstraintValidator<AssertTrue, Boolean> {

    @Override
    public void initialize(AssertTrue annotation) {
        //
    }

    public boolean isValid(Boolean value, ConstraintValidatorContext context) {
        return value == null || value;
    }

}