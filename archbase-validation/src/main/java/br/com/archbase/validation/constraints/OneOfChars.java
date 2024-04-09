package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.OneOfCharsValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Constraint(validatedBy = OneOfCharsValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
public @interface OneOfChars {
    String message() default "{br.com.archbase.bean.validation.constraints.ONE_OF_CHARS.message}";

    char[] value() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
