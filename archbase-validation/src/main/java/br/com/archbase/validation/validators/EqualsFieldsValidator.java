package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.EqualsFields;
import lombok.SneakyThrows;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

public class EqualsFieldsValidator implements ConstraintValidator<EqualsFields, Object> {

    private String baseField;
    private String matchField;

    @Override
    public void initialize(EqualsFields constraint) {
        baseField = constraint.baseField();
        matchField = constraint.matchField();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        try {
            Object baseFieldValue = getFieldValue(object, baseField);
            Object matchFieldValue = getFieldValue(object, matchField);
            return baseFieldValue != null && baseFieldValue.equals(matchFieldValue);
        } catch (Exception e) {
            // log error
            return false;
        }
    }

    @SneakyThrows
    @SuppressWarnings("all")
    private Object getFieldValue(Object object, String fieldName) {
        Class<?> clazz = object.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

}