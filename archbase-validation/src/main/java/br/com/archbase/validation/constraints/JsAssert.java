package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.JsAssertValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Repeatable(JsAsserts.class)
@Documented
@Constraint(validatedBy = JsAssertValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface JsAssert {
    String message() default "{br.com.archbase.bean.validation.constraints.EXP_JS.message}";

    String value() default "true";

    String attributeName() default "_";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}


