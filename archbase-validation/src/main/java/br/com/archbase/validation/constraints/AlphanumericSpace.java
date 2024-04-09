package br.com.archbase.validation.constraints;


import br.com.archbase.validation.validators.AlphanumericSpaceValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verifica se a String contém apenas letras, dígitos ou espaço ('') unicode.
 * Comparado com a anotação @Alphanumeric, a string vazia também é aceita.
 */
@Documented
@Constraint(validatedBy = AlphanumericSpaceValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface AlphanumericSpace {
    String message() default "{br.com.archbase.bean.validation.constraints.ALPHA_NUMERIC_SPACE.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
