package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated element has to represent a valid
 * credit card number. This is the Luhn algorithm implementation
 * which aims to check for user mistake, not credit card validity!
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
@LuhnCheck
public @interface CreditCardNumber {
    String message() default "{br.com.archbase.bean.validation.constraints.CreditCardNumber.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return Se caracteres diferentes de dígitos na entrada validada devem ser ignorados ({@code true}) ou resultar em um
     * erro de validação ({@code false}). O padrão é {@code false}
     */
    @OverridesAttribute(constraint = LuhnCheck.class, name = "ignoreNonDigitCharacters")
    boolean ignoreNonDigitCharacters() default false;

    /**
     * Define várias anotações {@code @CreditCardNumber} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        CreditCardNumber[] value();
    }
}
