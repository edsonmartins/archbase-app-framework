package br.com.archbase.validation.constraints;


import br.com.archbase.validation.validators.DomainValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * -
 * TODO - Esta classe NÃO faz parte da especificação bean_validation e pode desaparecer
 * assim que uma versão final da especificação contenha uma funcionalidade semelhante.
 * -
 * </p>
 * Descrição: anotação para validar um java.io.File é um diretório <br/>
 */
@Documented
@Constraint(validatedBy = DomainValidator.class)
@Target({FIELD, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
public @interface Domain {

    Class<?>[] groups() default {};

    String message() default "{br.com.archbase.bean.validation.constraints.DOMAIN.message}";

    Class<? extends Payload>[] payload() default {};

    boolean allowLocal() default false;
}