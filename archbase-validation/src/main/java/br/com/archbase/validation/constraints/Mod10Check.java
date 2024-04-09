package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <a href="http://en.wikipedia.org/wiki/Luhn_algorithm"> @Modulo 10 </a> restrição de verificação.
 * <p>
 * Permite validar que uma série de dígitos passa na soma de verificação Mod10
 * algoritmo. O clássico Mod10 é calculado somando os dígitos, com cada ímpar
 * valor do dígito (da direita para a esquerda) multiplicado por um {@code multiplier}.
 * Como exemplo, o ISBN-13 é a soma de verificação do Módulo 10 com {@code multiplier = 3}.
 * </p>
 * <p>
 * Existem casos conhecidos de códigos que usam multiplicadores para pares e ímpares
 * dígitos; Para suportar este tipo de implementações, a restrição Mod10 usa o
 * opção {@code weight}, que tem o mesmo efeito que o multiplicador, mas para
 * números.
 * </p>
 * <p>
 * O tipo compatível é {@code CharSequence}. {@code null} é considerado válido.
 * </p>
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface Mod10Check {
    String message() default "{br.com.archbase.bean.validation.constraints.Mod10Check.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return O multiplicador a ser usado para dígitos ímpares ao calcular a soma de verificação Mod10.
     */
    int multiplier() default 3;

    /**
     * @return O peso a ser usado para dígitos pares ao calcular a soma de verificação Mod10.
     */
    int weight() default 1;

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
     * o seguinte deve ser verdadeiro: <br>
     * {@code checkDigitIndex> 0 && (checkDigitIndex <startIndex || checkDigitIndex> = endIndex}.
     */
    int checkDigitIndex() default -1;

    /**
     * @return Se caracteres diferentes de dígitos na entrada validada devem ser ignorados ({@code true}) ou resultar em um
     * erro de validação ({@code false}).
     */
    boolean ignoreNonDigitCharacters() default true;

    /**
     * Define várias anotações {@code @ Mod10Check} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        Mod10Check[] value();
    }
}
