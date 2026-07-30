package br.com.archbase.offline.sync.spi;

/**
 * Fornece o id do usuário corrente para a auditoria do ledger de idempotência
 * ({@code processed_sync_operation.user_id}). O app implementa como bean
 * (normalmente resolvendo o usuário autenticado do contexto de segurança).
 *
 * <p>É OPCIONAL: quando nenhum bean é publicado, o executor grava {@code user_id}
 * nulo (comportamento retrocompatível com quem só tinha {@link SyncTenantProvider}).
 * Ao contrário do tenant (que segrega os dados), o usuário aqui é metadado de
 * auditoria — saber QUEM originou cada operação (multi-promotor no mesmo tenant,
 * debug/suporte, compliance). Ver {@link SyncTenantProvider}.
 */
@FunctionalInterface
public interface SyncUserProvider {

    /**
     * @return id do usuário/promotor corrente, ou {@code null} se indisponível
     *         (não deve lançar — em erro, retornar {@code null}).
     */
    String currentUserId();
}
