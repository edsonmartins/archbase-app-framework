package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate that the string is a valid URL.
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@ReportAsSingleViolation
@Pattern(regexp = "")
/**
 * {@code URL} é uma restrição de nível de campo / método que pode ser aplicada em uma string para afirmar que a string
 * representa um URL válido. Por padrão, a restrição verifica se o valor anotado está em conformidade com
 * <a href="http://www.ietf.org/rfc/rfc2396.txt"> RFC2396 </a>. Por meio dos parâmetros {@code protocol}, {@code host}
 * e {@code port} pode-se afirmar as partes correspondentes do URL analisado.
 * <p>
 * Devido ao fato de que RFC2396 é uma especificação muito genérica, muitas vezes não é restritiva o suficiente para um determinado caso de uso.
 * Nesse caso, também é possível especificar os parâmetros opcionais {@code regexp} e {@code flags}. Desta forma, um adicional
 * A expressão regular Java pode ser especificada com a string (URL) que deve corresponder.
 * </p>
 */
public @interface URL {
    String message() default "{br.com.archbase.bean.validation.constraints.URL.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return o protocolo (esquema) que a string anotada deve corresponder, por exemplo, ftp ou http.
     * Por padrão, qualquer protocolo é permitido
     */
    String protocol() default "";

    /**
     * @return the host que a string anotada deve corresponder, por exemplo, localhost. Por padrão, qualquer host é permitido
     */
    String host() default "";

    /**
     * @return a porta que a string anotada deve corresponder, por exemplo, 80. Por padrão, qualquer porta é permitida
     */
    int port() default -1;

    /**
     * @return uma expressão regular adicional que o URL anotado deve corresponder. O padrão é qualquer string ('. *')
     */
    @OverridesAttribute(constraint = Pattern.class, name = "regexp") String regexp() default ".*";

    /**
     * @return usado em combinação com {@link #regexp ()} para especificar uma opção de expressão regular
     */
    @OverridesAttribute(constraint = Pattern.class, name = "flags") Pattern.Flag[] flags() default {};

    /**
     * Define várias anotações {@code @URL} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        URL[] value();
    }
}
