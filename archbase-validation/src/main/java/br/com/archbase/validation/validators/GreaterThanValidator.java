package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.GreaterThan;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Comparator;

public class GreaterThanValidator implements ConstraintValidator<GreaterThan, Object> {


    private String field;
    private String greaterThan;
    private Comparator<Object> comparator;

    @Override
    public void initialize(GreaterThan ann) {

        field = ann.field();
        greaterThan = ann.greaterThan();
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
        Field greaterThanObj = null;
        try {
            fieldObj = validateThis.getClass().getDeclaredField(field);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nome de campo inválido", e);
        }
        try {
            greaterThanObj = validateThis.getClass().getDeclaredField(greaterThan);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nome greaterThan inválido", e);
        }
        if (fieldObj == null || greaterThanObj == null) {
            throw new IllegalArgumentException("Nomes de campo inválidos");
        }

        try {
            fieldObj.setAccessible(true);
            greaterThanObj.setAccessible(true);
            Object fieldVal = fieldObj.get(validateThis);
            Object largerThanVal = greaterThanObj.get(validateThis);
            return fieldVal == null && largerThanVal == null
                    || fieldVal != null && largerThanVal != null
                    && comparator.compare(fieldVal, largerThanVal) > 0;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não é possível validar o objeto", e);
        }

    }


}
