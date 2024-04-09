package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.AsciiPrintableValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verifica se a string contém apenas caracteres ASCII imprimíveis.
 */
@Documented
@Constraint(validatedBy = AsciiPrintableValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface AsciiPrintable {
    String message() default "{br.com.archbase.bean.validation.constraints.ASCII_PRINTABLE.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}