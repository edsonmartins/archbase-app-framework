package br.com.archbase.modulith.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma classe como um módulo no contexto de Modular Monolith.
 * <p>
 * Um módulo representa um Bounded Context do DDD, contendo toda a funcionalidade
 * necessária para um domínio de negócio específico.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @Module(
 *     name = "orders",
 *     version = "1.0.0",
 *     dependsOn = {"customers", "inventory"},
 *     description = "Order management module"
 * )
 * @Configuration
 * public class OrderModule implements ModuleLifecycle {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Module {

    /**
     * Nome único do módulo.
     * Deve ser um identificador válido em lowercase, usando hífens para separar palavras.
     * Exemplo: "orders", "customer-management", "inventory"
     */
    String name();

    /**
     * Versão do módulo no formato semver (X.Y.Z).
     * Usada para versionamento de APIs e controle de compatibilidade.
     */
    String version() default "1.0.0";

    /**
     * Lista de módulos dos quais este módulo depende.
     * O framework validará se todas as dependências estão disponíveis na inicialização.
     */
    String[] dependsOn() default {};

    /**
     * Descrição do módulo para documentação e monitoramento.
     */
    String description() default "";

    /**
     * Indica se o módulo está habilitado.
     * Módulos desabilitados não serão carregados na inicialização.
     */
    boolean enabled() default true;

    /**
     * Ordem de inicialização do módulo.
     * Módulos com valores menores são inicializados primeiro.
     */
    int order() default 0;
}
