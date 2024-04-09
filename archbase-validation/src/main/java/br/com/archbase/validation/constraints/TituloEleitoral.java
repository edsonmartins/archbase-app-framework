package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.TituloEleitoralValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;


/**
 * Restrição que pode ser associada a objetos em que o método
 * {@linkplain #toString()} represente um Título Eleitoral.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD})
@Constraint(validatedBy = TituloEleitoralValidator.class)
public @interface TituloEleitoral {
    String message() default "{br.com.archbase.bean.validation.constraints.TituloEleitoral.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
