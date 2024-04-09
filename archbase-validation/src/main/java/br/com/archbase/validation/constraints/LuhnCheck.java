package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Restrição de verificação do algoritmo de Luhn.
 * <p>
 * Permite validar que uma série de dígitos passa na soma de verificação Luhn Módulo 10
 * algoritmo. O Luhn Mod10 é calculado somando os dígitos, com cada ímpar
 * dígito (da direita para a esquerda) valor multiplicado por 2, se o valor for maior que 9 o
 * os dígitos do resultado são somados antes da soma total.
 * </p>
 * <p>
 * O tipo compatível é {@code CharSequence}. {@code null} é considerado válido.
 * </p>
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface LuhnCheck {
    String message() default "{br.com.archbase.bean.validation.constraints.LuhnCheck.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return o índice inicial (inclusive) para calcular a soma de verificação. Se não for especificado, 0 será assumido.
     */
    int startIndex() default 0;

    /**
     * @return o índice final (inclusive) para calcular a soma de verificação. Se não for especificado, o valor inteiro é considerado.
     */
    int endIndex() default Integer.MAX_VALUE;

    /**
     * @return O índice do dígito de verificação na entrada. Por padrão, é assumido que o dígito de verificação é o último
     * dígito do intervalo especificado. Se definido, o dígito no índice especificado é usado. Se definido
     * o seguinte deve ser verdadeiro:
     * {@code checkDigitIndex> 0 && (checkDigitIndex <startIndex || checkDigitIndex> = endIndex}.
     */
    int checkDigitIndex() default -1;

    /**
     * @return Se caracteres diferentes de dígitos na entrada validada devem ser ignorados ({@code true}) ou resultar em um
     * erro de validação ({@code false}).
     */
    boolean ignoreNonDigitCharacters() default true;

    /**
     * Define várias anotações {@code @LuhnCheck} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        LuhnCheck[] value();
    }
}
