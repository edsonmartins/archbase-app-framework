package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.LengthValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Valide se a string está entre o mínimo e o máximo incluído.
 */
@Documented
@Constraint(validatedBy = {LengthValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface Length {
    int min() default 0;

    int max() default Integer.MAX_VALUE;

    String message() default "{br.com.archbase.bean.validation.constraints.Length.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Define várias anotações {@code @Length} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        Length[] value();
    }
}
