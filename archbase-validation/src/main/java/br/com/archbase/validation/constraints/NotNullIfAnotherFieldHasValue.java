package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.NotNullIfAnotherFieldHasValueValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Valida que os campos [] {@code dependFieldNames} não são nulos se o campo {@code fieldName} tiver o valor {@code fieldValue}.
 */
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = NotNullIfAnotherFieldHasValueValidator.class)
@Documented
public @interface NotNullIfAnotherFieldHasValue {

    String fieldName();

    String fieldValue();

    String[] dependFieldName();

    String message() default "{br.com.archbase.bean.validation.constraints.NotNullIfAnotherFieldHasValue.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({TYPE, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    @Documented
    @interface List {
        NotNullIfAnotherFieldHasValue[] value();
    }
}