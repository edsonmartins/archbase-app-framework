package br.com.archbase.modulith.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma interface ou classe como API pública de um módulo.
 * <p>
 * APIs públicas são o contrato que outros módulos podem usar para comunicação.
 * Mudanças em APIs públicas devem seguir regras de versionamento semântico.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @ModuleApi(version = "1.0.0")
 * public interface OrderModuleApi {
 *     OrderDto createOrder(CreateOrderCommand command);
 *     Optional<OrderDto> getOrder(String orderId);
 * }
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModuleApi {

    /**
     * Versão da API no formato semver.
     * Mudanças breaking devem incrementar a versão major.
     */
    String version() default "1.0.0";

    /**
     * Indica se a API está deprecada.
     * APIs deprecadas devem incluir uma mensagem indicando a alternativa.
     */
    boolean deprecated() default false;

    /**
     * Mensagem de depreciação indicando a alternativa recomendada.
     */
    String deprecationMessage() default "";

    /**
     * Versão na qual a API foi introduzida.
     */
    String since() default "";

    /**
     * Descrição da API para documentação.
     */
    String description() default "";
}
