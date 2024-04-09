package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.EndsWithValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = EndsWithValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface EndsWith {
    String message() default "{br.com.archbase.bean.validation.constraints.ENDS_WITH.message}";

    String[] value();

    boolean ignoreCase() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
