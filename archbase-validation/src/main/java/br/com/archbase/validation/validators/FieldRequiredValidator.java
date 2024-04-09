package br.com.archbase.validation.validators;


import br.com.archbase.validation.constraints.Required;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Date;

public class FieldRequiredValidator implements ConstraintValidator<Required, Object> {

    @Override
    public void initialize(Required annotation) {
        //
    }

    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null)
            return false;
        if (value instanceof Date) {
            return ((Date) value).getTime() > 0;
        } else if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).getTime() > 0;
        } else if (value instanceof Collection<?>) {
            return !((Collection<?>) value).isEmpty();
        } else if (value instanceof String) {
            return !StringUtils.isEmpty((String) value);
        }
        return true;
    }

}
