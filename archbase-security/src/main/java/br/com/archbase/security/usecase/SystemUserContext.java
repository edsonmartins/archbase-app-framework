package br.com.archbase.security.usecase;

import java.util.function.Supplier;

/**
 * Interface para executar operações como um usuário do sistema.
 * Útil para cenários onde não há autenticação, como webhooks,
 * processamento de filas, jobs agendados, etc.
 */
public interface SystemUserContext {

    /**
     * Executa uma operação como usuário do sistema e retorna um resultado.
     *
     * @param systemUserEmail Email do usuário do sistema a ser utilizado
     * @param operation       Operação a ser executada
     * @param <T>             Tipo do resultado
     * @return Resultado da operação
     */
    <T> T runAsSystemUser(String systemUserEmail, Supplier<T> operation);

    /**
     * Executa uma operação como usuário do sistema sem retorno.
     *
     * @param systemUserEmail Email do usuário do sistema a ser utilizado
     * @param operation       Operação a ser executada
     */
    void runAsSystemUser(String systemUserEmail, Runnable operation);

    /**
     * Define manualmente o usuário do sistema no contexto de segurança.
     * IMPORTANTE: Após o processamento, chame clearSystemUserContext() para limpar o contexto.
     *
     * @param systemUserEmail Email do usuário do sistema a ser utilizado
     */
    void setSystemUserContext(String systemUserEmail);

    /**
     * Limpa o contexto de segurança do usuário do sistema.
     * Deve ser chamado após setSystemUserContext() quando o processamento terminar.
     */
    void clearSystemUserContext();

    /**
     * Verifica se o contexto atual é de um usuário do sistema.
     *
     * @return true se o contexto atual é de um usuário do sistema
     */
    boolean isSystemUserContext();
}
