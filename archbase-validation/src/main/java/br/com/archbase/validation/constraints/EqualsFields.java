package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.EqualsFieldsValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {EqualsFieldsValidator.class})
public @interface EqualsFields {

    String message() default "{br.com.archbase.bean.validation.constraints.EqualsFields.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String baseField();

    String matchField();

}