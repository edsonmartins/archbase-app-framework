package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.FromDateBeforeOrSameAsToDateValidator;

import jakarta.validation.Constraint;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FromDateBeforeOrSameAsToDateValidator.class)
@Documented
public @interface FromDateBeforeOrSameAsToDate {

    String message() default "{br.com.archbase.bean.validation.constraints.fromToDate.message}";

    Class[] groups() default {};

    Class[] payload() default {};

    String fromDate();

    String toDate();

    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        FromDateBeforeOrSameAsToDate[] value();
    }
}
