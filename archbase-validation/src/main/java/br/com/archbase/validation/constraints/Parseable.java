package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.ParseableValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ParseableValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Parseable {
    ParseableType value();

    String message() default "{br.com.archbase.bean.validation.constraints.PARSEABLE.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
