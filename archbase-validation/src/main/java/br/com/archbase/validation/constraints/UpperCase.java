package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.UpperCaseValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verifica se a String contém apenas caracteres maiúsculos.
 */
@Documented
@Constraint(validatedBy = UpperCaseValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface UpperCase {
    String message() default "{br.com.archbase.bean.validation.constraints.UPPERCASE.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}