package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated element has to be in the appropriate range. Apply on numeric values or string
 * representation of the numeric value.
 */
@Documented
@Constraint(validatedBy = {})
@Target({TYPE, METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Min(0)
@Max(Long.MAX_VALUE)
@ReportAsSingleViolation
public @interface Range {
    @OverridesAttribute(constraint = Min.class, name = "value") long min() default 0;

    @OverridesAttribute(constraint = Max.class, name = "value") long max() default Long.MAX_VALUE;

    String message() default "{br.com.archbase.bean.validation.constraints.Range.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Define várias anotações {@code @Range} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        Range[] value();
    }
}
