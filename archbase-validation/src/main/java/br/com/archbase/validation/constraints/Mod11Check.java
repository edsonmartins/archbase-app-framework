package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Modulo 11 verificar restrição.
 * <p>
 * Permite validar que uma série de dígitos passa na soma de verificação Mod11
 * algoritmo.
 * Para a variante Mod11 mais comum, o cálculo da soma é feito multiplicando um peso de
 * o dígito mais à direita (excluindo o dígito de verificação) à esquerda. O peso
 * começa com 2 e aumenta em 1 para cada dígito. Então o resultado é usado para
 * calcule o dígito de verificação usando {@code 11 - (soma% 11)}.
 * </p>
 * <p>
 * Exemplo: o dígito de verificação para 24187 é 3 <br>
 * Soma = 7x2 + 8x3 + 1x4 + 4x5 + 2x6 = 74 <br>
 * 11 - (74% 11) = 11 - 8 = 3, então "24187-3" é uma sequência de caracteres válida.
 * </p>
 * <p>
 * O cálculo do Mod11 pode resultar em 10 ou 11; por padrão 10 é tratado como
 * {@code 'X'} e 11 como {@code '0'}, este comportamento pode ser alterado usando o
 * opções {@code treatCheck10As} e {@code treatCheck10As}.
 * </p>
 * <p>
 * Algumas implementações fazem o cálculo da soma na ordem inversa (da esquerda para a direita);
 * especifique a direção de processamento {@link ProcessingDirection # LEFT_TO_RIGHT} em
 * este caso.
 * </p>
 * <p>
 * O tipo compatível é {@code CharSequence}. {@code null} é considerado válido.
 * </p>
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface Mod11Check {
    String message() default "{br.com.archbase.bean.validation.constraints.Mod11Check.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return O limite para o crescimento do multiplicador do algoritmo Mod11, se nenhum valor for especificado, o multiplicador crescerá indefinidamente
     */
    int threshold() default Integer.MAX_VALUE;

    /**
     * @return o índice inicial (inclusive) para calcular a soma de verificação. Se não for especificado, 0 será assumido.
     */
    int startIndex() default 0;

    /**
     * @return o índice final (inclusive) para calcular a soma de verificação. Se não for especificado, o valor inteiro é considerado
     */
    int endIndex() default Integer.MAX_VALUE;

    /**
     * @return O índice do dígito de verificação na entrada. Por padrão, é assumido que o dígito de verificação é o último
     * dígito do intervalo especificado. Se definido, o dígito no índice especificado é usado. Se definido
     * o seguinte deve ser verdadeiro: <br>
     * {@code checkDigitIndex> 0 && (checkDigitIndex <startIndex || checkDigitIndex> = endIndex}.
     */
    int checkDigitIndex() default -1;

    /**
     * @return Se caracteres diferentes de dígitos na entrada validada devem ser ignorados ({@code true}) ou resultar em um
     * erro de validação ({@code false}).
     */
    boolean ignoreNonDigitCharacters() default false;

    /**
     * @return O {@code char} que representa o dígito de verificação quando o Mod11
     * checksum igual a 10. Se não for especificado, {@code 'X'} será assumido.
     */
    char treatCheck10As() default 'X';

    /**
     * @return O {@code char} que representa o dígito de verificação quando o Mod11
     * checksum igual a 11. Se não for especificado, {@code '0'} será assumido.
     */
    char treatCheck11As() default '0';

    /**
     * @return Retorna {@code RIGHT_TO_LEFT} se a soma de verificação Mod11 deve ser feita do dígito mais à direita para o mais à esquerda.
     * por exemplo. Código 12345- ?:
     * <ul>
     * <li> {@code RIGHT_TO_LEFT} a soma (5 * 2 + 4 * 3 + 3 * 4 + 2 * 5 + 1 * 6) com o dígito de verificação 5 </li>
     * <li> {@code LEFT_TO_RIGHT} a soma (1 * 2 + 2 * 3 + 3 * 4 + 4 * 5 + 5 * 6) com o dígito de verificação 7 </li>
     * </ul>
     * Se não for especificado, {@code RIGHT_TO_LEFT} é assumido, é o comportamento Mod11 padrão.
     */
    ProcessingDirection processingDirection() default ProcessingDirection.RIGHT_TO_LEFT;

    public enum ProcessingDirection {
        RIGHT_TO_LEFT,
        LEFT_TO_RIGHT
    }

    /**
     * Define várias anotações {@code @ Mod11Check} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        Mod11Check[] value();
    }
}
