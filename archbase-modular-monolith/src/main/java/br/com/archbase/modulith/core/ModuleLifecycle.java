package br.com.archbase.modulith.core;

/**
 * Interface para gerenciamento do ciclo de vida de um módulo.
 * <p>
 * Módulos que precisam executar lógica durante inicialização ou
 * encerramento devem implementar esta interface.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @Module(name = "orders")
 * @Configuration
 * public class OrderModule implements ModuleLifecycle {
 *
 *     @Override
 *     public void onStart(ModuleContext context) {
 *         // Inicializar recursos, validar configurações, etc.
 *     }
 *
 *     @Override
 *     public void onStop() {
 *         // Liberar recursos, fechar conexões, etc.
 *     }
 * }
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public interface ModuleLifecycle {

    /**
     * Chamado quando o módulo está sendo inicializado.
     * <p>
     * Neste ponto, todas as dependências do módulo já foram inicializadas
     * e estão disponíveis através do ModuleContext.
     *
     * @param context Contexto do módulo com acesso a recursos e outros módulos
     */
    default void onStart(ModuleContext context) {
        // Implementação padrão vazia
    }

    /**
     * Chamado quando o módulo está sendo parado.
     * <p>
     * Este método deve liberar todos os recursos alocados pelo módulo.
     */
    default void onStop() {
        // Implementação padrão vazia
    }

    /**
     * Chamado quando há falha na inicialização do módulo.
     *
     * @param cause Exceção que causou a falha
     */
    default void onError(Throwable cause) {
        // Implementação padrão vazia
    }

    /**
     * Retorna true se o módulo está pronto para receber requisições.
     * <p>
     * Este método é usado pelos health checks para verificar a saúde do módulo.
     *
     * @return true se o módulo está saudável
     */
    default boolean isHealthy() {
        return true;
    }
}
