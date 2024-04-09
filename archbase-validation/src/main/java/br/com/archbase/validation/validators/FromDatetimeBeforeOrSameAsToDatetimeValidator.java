package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.ReflectionUtils;
import br.com.archbase.validation.constraints.FromDateBeforeOrSameAsToDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class FromDatetimeBeforeOrSameAsToDatetimeValidator
        implements ConstraintValidator<FromDateBeforeOrSameAsToDate, Object> {

    private String fromDate;

    private String toDate;

    public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
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

        LocalDateTime fromLocalDate = null;
        LocalDateTime toLocalDate = null;
        if (value1 != null)
            fromLocalDate = (value1 instanceof LocalDateTime ? (LocalDateTime) value1 : convertToLocalDateTimeViaInstant((Date) value1));
        if (value2 != null)
            toLocalDate = (value2 instanceof LocalDateTime ? (LocalDateTime) value2 : convertToLocalDateTimeViaInstant((Date) value2));

        if (fromLocalDate == null || toLocalDate == null) {
            return true;
        }

        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext.buildConstraintViolationWithTemplate(
                "{br.com.archbase.bean.validation.constraints.fromToDatetime.message}")
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