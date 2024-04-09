package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verifica se a sequência de caracteres anotada é válida
 * Número <a href="http://en.wikipedia.org/wiki/International_Article_Number_%28EAN%29"> EAN 13 </a>. O comprimento do
 * número e o dígito de verificação são verificados
 *
 * <p>
 * O tipo compatível é {@code CharSequence}. {@code null} é considerado válido.
 * </p>
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
@Mod10Check
public @interface EAN {
    String message() default "{br.com.archbase.bean.validation.constraints.EAN.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Type type() default Type.EAN13;

    public enum Type {
        EAN13,
        EAN8
    }

    /**
     * Defines several {@code @NotBlank} annotations on the same element.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        EAN[] value();
    }
}
