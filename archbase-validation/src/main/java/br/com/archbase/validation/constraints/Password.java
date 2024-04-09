package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.PasswordValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Password {

    boolean containsUpperCase() default true;

    boolean containsLowerCase() default true;

    boolean containsSpecialChar() default true;

    boolean containsDigits() default true;

    boolean allowSpace() default false;

    int minSize() default 8;

    int maxSize() default 32;

    String message() default "{br.com.archbase.bean.validation.constraints.PASSWORD.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
