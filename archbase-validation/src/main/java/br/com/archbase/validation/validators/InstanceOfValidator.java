package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.InstanceOf;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class InstanceOfValidator implements ConstraintValidator<InstanceOf, Object> {

    private InstanceOf annotation;

    @Override
    public void initialize(InstanceOf constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (null == value) {
            return false;
        }
        return Arrays.stream(annotation.value()).anyMatch(item -> item.isInstance(value));
    }
}
