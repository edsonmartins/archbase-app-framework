package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.FieldRequiredValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
@Constraint(validatedBy = {FieldRequiredValidator.class})
public @interface Required {

    String message() default "{br.com.archbase.bean.validation.constraints.Required.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
