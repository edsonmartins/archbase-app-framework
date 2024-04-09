package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.NotInstanceOf;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotInstanceOfValidator implements ConstraintValidator<NotInstanceOf, Object> {

    private NotInstanceOf annotation;

    @Override
    public void initialize(NotInstanceOf constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {

        if (null == value) {
            return false;
        }

        Class<?>[] classes = annotation.value();

        for (Class<?> cls : classes) {
            if (cls.isInstance(value)) {
                return false;
            }
        }

        return true;
    }
}
