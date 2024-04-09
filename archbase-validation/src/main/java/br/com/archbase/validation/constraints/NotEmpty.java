package br.com.archbase.validation.constraints;

import br.com.archbase.validation.validators.NotEmptyValidator;
import br.com.archbase.validation.validators.NotEmptyValidatorForCollection;
import br.com.archbase.validation.validators.NotEmptyValidatorForMap;
import br.com.archbase.validation.validators.NotEmptyValidatorForString;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <pre>
 * Esta classe NÃO faz parte da especificação bean_validation e pode desaparecer
 * assim que uma versão final da especificação contenha uma funcionalidade semelhante.
 * </pre>
 */
@Documented
@Constraint(
        validatedBy = {NotEmptyValidatorForCollection.class, NotEmptyValidatorForMap.class,
                NotEmptyValidatorForString.class, NotEmptyValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
public @interface NotEmpty {
    Class<?>[] groups() default {};

    String message() default "{br.com.archbase.bean.validation.constraints.NotEmpty.message}";

    Class<? extends Payload>[] payload() default {};
}
