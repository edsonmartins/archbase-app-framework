package br.com.archbase.modulith.communication;

import br.com.archbase.modulith.communication.contracts.ModuleRequest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Gateway para comunicação síncrona entre módulos.
 * <p>
 * O ModuleGateway fornece um mecanismo para módulos executarem
 * operações em outros módulos de forma síncrona.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * // Executar requisição
 * CustomerDto customer = moduleGateway.execute("customers", new GetCustomerRequest("123"));
 *
 * // Executar com timeout
 * CustomerDto customer = moduleGateway.execute("customers", request, Duration.ofSeconds(5));
 *
 * // Verificar disponibilidade
 * if (moduleGateway.isModuleAvailable("customers")) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public interface ModuleGateway {

    /**
     * Executa uma requisição em outro módulo.
     *
     * @param targetModule Nome do módulo de destino
     * @param request      Requisição a ser executada
     * @param <R>          Tipo do resultado
     * @return Resultado da execução
     * @throws ModuleNotFoundException se o módulo não estiver disponível
     * @throws ModuleRequestException  se houver erro na execução
     */
    <R> R execute(String targetModule, ModuleRequest<R> request);

    /**
     * Executa uma requisição em outro módulo com timeout.
     *
     * @param targetModule Nome do módulo de destino
     * @param request      Requisição a ser executada
     * @param timeout      Tempo máximo de espera
     * @param <R>          Tipo do resultado
     * @return Resultado da execução
     * @throws ModuleNotFoundException    se o módulo não estiver disponível
     * @throws ModuleRequestException     se houver erro na execução
     * @throws ModuleRequestTimeoutException se o timeout for excedido
     */
    <R> R execute(String targetModule, ModuleRequest<R> request, Duration timeout);

    /**
     * Executa uma requisição de forma assíncrona.
     *
     * @param targetModule Nome do módulo de destino
     * @param request      Requisição a ser executada
     * @param <R>          Tipo do resultado
     * @return Future com o resultado
     */
    <R> CompletableFuture<R> executeAsync(String targetModule, ModuleRequest<R> request);

    /**
     * Verifica se um módulo está disponível para receber requisições.
     *
     * @param moduleName Nome do módulo
     * @return true se o módulo está disponível
     */
    boolean isModuleAvailable(String moduleName);

    /**
     * Aguarda até que um módulo esteja disponível.
     *
     * @param moduleName Nome do módulo
     * @param timeout    Tempo máximo de espera
     * @return true se o módulo ficou disponível dentro do timeout
     */
    boolean waitForModule(String moduleName, Duration timeout);
}
