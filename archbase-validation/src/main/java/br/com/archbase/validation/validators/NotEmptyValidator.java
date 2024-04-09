package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.NotEmpty;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Descrição: Verifique o não vazio de um
 * qualquer objeto que tenha um método público isEmpty(): booleano ou um método toString() válido
 */
public class NotEmptyValidator implements ConstraintValidator<NotEmpty, Object> {

    @Override
    public void initialize(NotEmpty constraintAnnotation) {
        // fazer nada
    }

    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (value.getClass().isArray()) {
            return Array.getLength(value) > 0;
        } else {
            try {
                Method isEmptyMethod = value.getClass().getMethod("isEmpty");
                if (isEmptyMethod != null) {
                    return !((Boolean) isEmptyMethod.invoke(value)).booleanValue();
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException iae) {
                // fazer nada
            }
            return value.toString().length() > 0;
        }
    }
}
