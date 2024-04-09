package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Restrição de verificação de módulo.
 * <p>
 * Permite validar que uma série de dígitos passa no algoritmo de checksum mod 10 ou mod 11.
 * </p>
 * <p>
 * O tipo compatível é {@code CharSequence}. {@code null} é considerado válido.
 * </p>
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface ModCheck {
    String message() default "{br.com.archbase.bean.validation.constraints.ModCheck.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return O algoritmo de módulo a ser usado
     */
    ModType modType();

    /**
     * @return O multiplicador a ser usado pelo algoritmo de mod escolhido
     */
    int multiplier();

    /**
     * @return o índice inicial (inclusive) para calcular a soma de verificação. Se não for especificado, 0 será assumido.
     */
    int startIndex() default 0;

    /**
     * @return o índice final (exclusivo) para calcular a soma de verificação. Se não for especificado, o valor inteiro é considerado
     */
    int endIndex() default Integer.MAX_VALUE;

    /**
     * @return A posição do dígito de verificação na entrada. Por padrão, assume-se que o dígito de verificação é parte do
     * intervalo especificado. Se definido, o dígito na posição especificada é usado como dígito de verificação. Se definido, o seguinte é válido
     * verdadeiro: {@code checkDigitPosition> 0 && (checkDigitPosition <startIndex || checkDigitPosition> = endIndex}.
     */
    int checkDigitPosition() default -1;

    /**
     * @return Retorna {@code true} se caracteres sem dígitos devem ser ignorados, {@code false} se caracteres sem dígitos
     * resulta em um erro de validação. {@code startIndex} e {@code endIndex} sempre se referem apenas a dígitos
     * personagens.
     */
    boolean ignoreNonDigitCharacters() default true;

    public enum ModType {
        /**
         * Representa um algoritmo MOD10 (também conhecido como algoritmo Luhn)
         */
        MOD10,
        /**
         * Representa um algoritmo MOD11. Um resto de 10 ou 11 no algoritmo é mapeado para o dígito de verificação 0.
         */
        MOD11
    }

    /**
     * Define várias anotações {@code @ModCheck} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        ModCheck[] value();
    }
}
