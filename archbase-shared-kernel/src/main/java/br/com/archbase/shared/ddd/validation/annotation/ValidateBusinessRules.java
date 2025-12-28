package br.com.archbase.shared.ddd.validation.annotation;

import br.com.archbase.shared.ddd.validation.BusinessRuleValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para marcar métodos que devem validar regras de negócio.
 * <p>
 * Pode ser usada em conjunto com interceptadores ou AOP para
 * validar automaticamente antes da execução do método.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * @ValidateBusinessRules
 * public void criarCliente(Cliente cliente) {
 *     // método só executa se validação passar
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateBusinessRules {

    /**
     * Mensagem de erro personalizada para lançar quando a validação falhar.
     *
     * @return Mensagem de erro
     */
    String message() default "Validação de negócio falhou";

    /**
     * Define se deve lançar exceção quando a validação falhar.
     *
     * @return true para lançar exceção
     */
    boolean throwOnError() default true;
}
