package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.AlphaSpaceValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verifica se a string contém apenas letras Unicode e espaço ('')
 */
@Documented
@Constraint(validatedBy = AlphaSpaceValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface AlphaSpace {
    String message() default "{br.com.archbase.bean.validation.constraints.ALPHA_SPACE.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}