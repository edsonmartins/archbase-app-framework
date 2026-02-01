package br.com.archbase.modulith.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Habilita o suporte a Modular Monolith na aplicação.
 * <p>
 * Esta anotação deve ser adicionada a uma classe de configuração para
 * ativar o registro automático de módulos, comunicação entre módulos
 * e health checks.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableModularMonolith(
 *     basePackages = "com.myapp.modules",
 *     validateDependenciesOnStartup = true
 * )
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
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
@Import(ModuleAutoConfiguration.class)
public @interface EnableModularMonolith {

    /**
     * Pacotes base para scan de módulos.
     * Se vazio, usa o pacote da classe anotada.
     */
    String[] basePackages() default {};

    /**
     * Se true, valida as dependências entre módulos durante a inicialização.
     */
    boolean validateDependenciesOnStartup() default true;

    /**
     * Se true, habilita os health checks de módulos.
     */
    boolean enableHealthChecks() default true;

    /**
     * Se true, registra métricas de módulos no Micrometer.
     */
    boolean enableMetrics() default true;

    /**
     * Modo de comunicação padrão entre módulos.
     */
    CommunicationMode defaultCommunicationMode() default CommunicationMode.IN_MEMORY;

    /**
     * Modos de comunicação suportados.
     */
    enum CommunicationMode {
        /**
         * Comunicação in-memory (mesmo processo).
         */
        IN_MEMORY,

        /**
         * Comunicação via eventos persistidos no Outbox.
         */
        OUTBOX,

        /**
         * Comunicação via message broker externo.
         */
        MESSAGE_BROKER
    }
}
