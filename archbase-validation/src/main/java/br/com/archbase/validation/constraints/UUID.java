package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.UUIDValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.FIELD)
@Constraint(validatedBy = {UUIDValidator.class})
@Retention(RUNTIME)
public @interface UUID {

    String message() default "{br.com.archbase.bean.validation.constraints.UUID.message}";

    UUIDPattern pattern() default UUIDPattern.UUID_PATTERN_4;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public enum UUIDPattern {
        UUID_PATTERN_1, UUID_PATTERN_2, UUID_PATTERN_3, UUID_PATTERN_4, UUID_PATTERN_5, UUID_PATTERN_6
    }
}


