package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.IEValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Restrição que pode ser associada a classes que contenham um objeto que
 * represente uma Inscricao Estadual e outro objeto identificando o estado a que
 * este documento pertence.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
@Constraint(validatedBy = IEValidator.class)
public @interface IE {
    String message() default "{br.com.archbase.bean.validation.constraints.IE.message}";

    String ieField() default "ie";

    String estadoField() default "estado";

    boolean formatted() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
