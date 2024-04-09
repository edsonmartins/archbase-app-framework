package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.LessOrEqualsThan;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Comparator;

public class LessOrEqualsThanValidator implements ConstraintValidator<LessOrEqualsThan, Object> {


    private String field;
    private String lessOrEqualsThan;
    private Comparator<Object> comparator;

    @Override
    public void initialize(LessOrEqualsThan ann) {

        field = ann.field();
        lessOrEqualsThan = ann.lessOrEqualsThan();
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
        Field lessOrEqualsThanObj = null;
        try {
            fieldObj = validateThis.getClass().getDeclaredField(field);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nome de campo inválido", e);
        }
        try {
            lessOrEqualsThanObj = validateThis.getClass().getDeclaredField(lessOrEqualsThan);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nome lessOrEqualsThan inválido", e);
        }
        if (fieldObj == null || lessOrEqualsThanObj == null) {
            throw new IllegalArgumentException("Nomes de campo inválidos");
        }

        try {
            fieldObj.setAccessible(true);
            lessOrEqualsThanObj.setAccessible(true);
            Object fieldVal = fieldObj.get(validateThis);
            Object largerThanVal = lessOrEqualsThanObj.get(validateThis);
            return fieldVal == null && largerThanVal == null
                    || fieldVal != null && largerThanVal != null
                    && comparator.compare(fieldVal, largerThanVal) <= 0;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não é possível validar o objeto", e);
        }

    }

}
