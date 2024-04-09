package br.com.archbase.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Operador booleano que é aplicado a todas as restrições de uma anotação de restrição de composição.
 * <p>
 * Uma anotação de restrição composta pode definir uma combinação booleana das restrições que a compõem,
 * usando {@code @ConstraintComposition}.
 * </p>
 */
@Documented
@Target({ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ConstraintComposition {
    /**
     * O valor deste elemento especifica o operador booleano,
     * a saber disjunção (OR), negação da conjunção (ALL_FALSE),
     * ou, o padrão, conjunção simples (AND).
     *
     * @return o valor {@code CompositionType}
     */
    CompositionType value() default CompositionType.AND;
}
