package br.com.archbase.validation.validators;


import br.com.archbase.validation.constraints.Before;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BeforeValidator implements ConstraintValidator<Before, Date> {

    private Before annotation;

    @Override
    public void initialize(Before constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Date value, ConstraintValidatorContext context) {

        if (value == null) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(annotation.format());
        try {
            Date beforeDate = sdf.parse(annotation.value());
            return value.before(beforeDate);
        } catch (ParseException e) {
            return false;
        }
    }
}
