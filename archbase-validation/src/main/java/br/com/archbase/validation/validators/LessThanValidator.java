package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.LessThan;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Comparator;

public class LessThanValidator implements ConstraintValidator<LessThan, Object> {


    private String field;
    private String lessThan;
    private Comparator<Object> comparator;

    @Override
    public void initialize(LessThan ann) {

        field = ann.field();
        lessThan = ann.lessThan();
        Class<? extends Comparator<Object>> comparatorClass = ann.comparator();
        try {
            comparator = comparatorClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Não é possível instanciar o comparador", e);
        }
    }

    @Override
    @SuppressWarnings("all")
    public boolean isValid(Object validateThis, ConstraintValidatorContext ctx) {
        if (validateThis == null) {
            throw new IllegalArgumentException("validateThis é nulo");
        }
        Field fieldObj = null;
        Field lessThanObj = null;
        try {
            fieldObj = validateThis.getClass().getDeclaredField(field);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nome de campo inválido", e);
        }
        try {
            lessThanObj = validateThis.getClass().getDeclaredField(lessThan);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nome lessThan inválido", e);
        }
        if (fieldObj == null || lessThanObj == null) {
            throw new IllegalArgumentException("Nomes de campo inválidos");
        }

        try {
            fieldObj.setAccessible(true);
            lessThanObj.setAccessible(true);
            Object fieldVal = fieldObj.get(validateThis);
            Object largerThanVal = lessThanObj.get(validateThis);
            return fieldVal == null && largerThanVal == null
                    || fieldVal != null && largerThanVal != null
                    && comparator.compare(fieldVal, largerThanVal) < 0;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não é possível validar o objeto", e);
        }

    }

}
