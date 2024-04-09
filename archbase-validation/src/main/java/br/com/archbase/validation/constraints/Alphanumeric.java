package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.AlphanumericValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verifica se a String contém apenas letras ou dígitos Unicode.
 */
@Documented
@Constraint(validatedBy = AlphanumericValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Alphanumeric {
    String message() default "{br.com.archbase.bean.validation.constraints.ALPHA_NUMERIC.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
