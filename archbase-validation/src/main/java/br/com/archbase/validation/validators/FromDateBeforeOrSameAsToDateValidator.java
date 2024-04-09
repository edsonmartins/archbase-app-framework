package br.com.archbase.validation.validators;


import br.com.archbase.shared.kernel.utils.ReflectionUtils;
import br.com.archbase.validation.constraints.FromDateBeforeOrSameAsToDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class FromDateBeforeOrSameAsToDateValidator
        implements ConstraintValidator<FromDateBeforeOrSameAsToDate, Object> {

    private String fromDate;

    private String toDate;

    private LocalDate convertToLocalDateViaMilisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    @Override
    public boolean isValid(Object requestObject, ConstraintValidatorContext constraintValidatorContext) {
        if (requestObject == null) {
            return true;
        }
        Field fromDateField = ReflectionUtils.getFieldByName(requestObject.getClass(), fromDate);
        Field toDateField = ReflectionUtils.getFieldByName(requestObject.getClass(), toDate);

        Object value1 = ReflectionUtils.getField(fromDateField, requestObject);
        Object value2 = ReflectionUtils.getField(toDateField, requestObject);

        LocalDate fromLocalDate = null;
        LocalDate toLocalDate = null;
        if (value1 != null)
            fromLocalDate = (value1 instanceof LocalDate ? (LocalDate) value1 : convertToLocalDateViaMilisecond((Date) value1));
        if (value2 != null)
            toLocalDate = (value2 instanceof LocalDate ? (LocalDate) value2 : convertToLocalDateViaMilisecond((Date) value2));

        if (fromLocalDate == null || toLocalDate == null) {
            return true;
        }

        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext.buildConstraintViolationWithTemplate(
                "{br.com.archbase.bean.validation.constraints.fromToDate.message}")
                .addPropertyNode(fromDate)
                .addConstraintViolation();

        return fromLocalDate.isEqual(toLocalDate) || fromLocalDate.isBefore(toLocalDate);
    }

    @Override
    public void initialize(FromDateBeforeOrSameAsToDate constraintAnnotation) {
        fromDate = constraintAnnotation.fromDate();
        toDate = constraintAnnotation.toDate();

    }
}