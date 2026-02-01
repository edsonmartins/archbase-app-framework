package br.com.archbase.modulith.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara explicitamente uma dependência entre módulos.
 * <p>
 * Pode ser usada para documentar e validar dependências em tempo de compilação
 * e execução. O framework validará que todas as dependências declaradas existem
 * e não formam ciclos.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @Module(name = "orders")
 * @ModuleDependency(module = "customers", type = DependencyType.SYNC)
 * @ModuleDependency(module = "inventory", type = DependencyType.ASYNC)
 * public class OrderModule {
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
@Repeatable(ModuleDependencies.class)
public @interface ModuleDependency {

    /**
     * Nome do módulo do qual este módulo depende.
     */
    String module();

    /**
     * Tipo de dependência.
     */
    DependencyType type() default DependencyType.SYNC;

    /**
     * Indica se a dependência é obrigatória.
     * Dependências opcionais não causarão falha se o módulo não estiver disponível.
     */
    boolean required() default true;

    /**
     * Descrição do motivo da dependência.
     */
    String reason() default "";

    /**
     * Tipos de dependência entre módulos.
     */
    enum DependencyType {
        /**
         * Dependência síncrona via chamada direta (ModuleGateway).
         * Requer que ambos os módulos estejam disponíveis simultaneamente.
         */
        SYNC,

        /**
         * Dependência assíncrona via eventos (IntegrationEventBus).
         * Permite eventual consistency entre módulos.
         */
        ASYNC,

        /**
         * Dependência de dados compartilhados (não recomendado).
         * Indica que os módulos compartilham tabelas ou esquemas.
         */
        SHARED_DATA
    }
}
