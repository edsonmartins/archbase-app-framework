package br.com.archbase.modulith.communication.contracts;

/**
 * Interface para handlers de requisições de módulo.
 *
 * @param <T> Tipo da requisição
 * @param <R> Tipo do resultado
 * @author Archbase Team
 * @since 3.0.0
 */
@FunctionalInterface
public interface ModuleRequestHandler<T extends ModuleRequest<R>, R> {

    /**
     * Processa a requisição e retorna o resultado.
     *
     * @param request Requisição a ser processada
     * @return Resultado do processamento
     */
    R handle(T request);
}
