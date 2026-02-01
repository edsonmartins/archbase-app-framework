package br.com.archbase.modulith.communication.contracts;

import java.io.Serializable;

/**
 * Interface base para requisições síncronas entre módulos.
 * <p>
 * ModuleRequest representa uma operação que um módulo deseja executar
 * em outro módulo, similar ao padrão Command/Query.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * public record GetCustomerRequest(String customerId)
 *     implements ModuleRequest<CustomerDto> {
 *
 *     @Override
 *     public String getTargetModule() {
 *         return "customers";
 *     }
 * }
 *
 * // Uso
 * CustomerDto customer = moduleGateway.execute("customers", new GetCustomerRequest("123"));
 * }
 * </pre>
 *
 * @param <R> Tipo do resultado esperado
 * @author Archbase Team
 * @since 3.0.0
 */
public interface ModuleRequest<R> extends Serializable {

    /**
     * Retorna o nome do módulo que deve processar esta requisição.
     */
    default String getTargetModule() {
        return null;
    }

    /**
     * Retorna o nome da operação.
     * Por padrão, usa o nome simples da classe.
     */
    default String getOperationName() {
        return getClass().getSimpleName();
    }

    /**
     * Indica se esta requisição é idempotente.
     * Requisições idempotentes podem ser reexecutadas com segurança.
     */
    default boolean isIdempotent() {
        return true;
    }
}
