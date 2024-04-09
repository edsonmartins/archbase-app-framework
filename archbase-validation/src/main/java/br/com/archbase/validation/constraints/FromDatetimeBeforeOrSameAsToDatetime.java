package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.FromDatetimeBeforeOrSameAsToDatetimeValidator;

import jakarta.validation.Constraint;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FromDatetimeBeforeOrSameAsToDatetimeValidator.class)
@Documented
public @interface FromDatetimeBeforeOrSameAsToDatetime {

    String message() default "{br.com.archbase.bean.validation.constraints.fromToDatetime.message}";

    Class[] groups() default {};

    Class[] payload() default {};

    String fromDate();

    String toDate();

    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        FromDatetimeBeforeOrSameAsToDatetime[] value();
    }
}
