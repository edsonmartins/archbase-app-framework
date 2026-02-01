package br.com.archbase.modulith.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma interface, classe ou método como API interna de um módulo.
 * <p>
 * APIs internas não devem ser acessadas por outros módulos.
 * O ArchUnit pode ser configurado para validar que apenas classes
 * do mesmo módulo acessam APIs internas.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @InternalApi
 * public class OrderDomainService {
 *     // Implementação interna que não deve ser exposta
 * }
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InternalApi {

    /**
     * Razão pela qual esta API é interna e não deve ser exposta.
     */
    String reason() default "";

    /**
     * Indica se o acesso externo deve gerar warning ou error nos testes de arquitetura.
     */
    Severity severity() default Severity.ERROR;

    enum Severity {
        WARNING,
        ERROR
    }
}
