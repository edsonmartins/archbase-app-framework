package br.com.archbase.validation.validators;


import br.com.archbase.validation.constraints.After;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AfterValidator implements ConstraintValidator<After, Date> {

    private After annotation;

    @Override
    public void initialize(After constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Date value, ConstraintValidatorContext context) {

        if (value == null) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(annotation.format());
        try {
            Date afterDate = sdf.parse(annotation.value());
            return value.after(afterDate);
        } catch (ParseException e) {
            return false;
        }
    }
}
