package br.com.archbase.validation.validators;



import br.com.archbase.validation.constraints.IsDate;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class IsDateValidator implements ConstraintValidator<IsDate, String> {

    private IsDate annotation;

    @Override
    public void initialize(IsDate constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (StringUtils.isEmpty(value)) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(annotation.value());
        try {
            sdf.parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
