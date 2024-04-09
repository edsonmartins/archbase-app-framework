package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.EmailValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * --
 * TODO - Esta classe NÃO faz parte da especificação bean_validation e pode desaparecer
 * assim que uma versão final da especificação contenha uma funcionalidade semelhante.
 * -
 * </p>
 * Descrição: anotação para validar um endereço de e-mail (por padrão) <br/>
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
public @interface Email {
    Class<?>[] groups() default {};

    String message() default "{br.com.archbase.bean.validation.constraints.Email.message}";

    Class<? extends Payload>[] payload() default {};
}
