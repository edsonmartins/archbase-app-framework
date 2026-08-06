package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncAckDTO;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;

/**
 * Executa UMA operação em transação própria. Porta explícita para (a) garantir
 * que a chamada passe pelo proxy Spring (REQUIRES_NEW) e (b) permitir fake em teste.
 */
public interface SyncOperationExecutorPort {

    /**
     * Aplica a operação numa transação isolada (efeito de domínio + ledger de
     * idempotência no MESMO commit — SYNC-004). Sucesso devolve o ACK
     * {@code PROCESSED}. Erros de negócio e transitórios PROPAGAM como exceção
     * ({@code SyncRejectedException}/{@code SyncConflictException}/
     * {@code SyncSkippedException}/demais {@code RuntimeException}) — a transação
     * reverte e quem chama ({@code SyncOperationProcessor}) traduz para ACK
     * DEPOIS do rollback, evitando o {@code UnexpectedRollbackException} do
     * catch-and-return dentro da transação.
     */
    SyncAckDTO execute(SyncOperationDTO operation);

    /**
     * Registra durablemente um {@code SKIPPED} (idempotência) em transação
     * própria, chamado pelo processador ao capturar {@code SyncSkippedException}.
     * Skip não tem efeito de domínio a preservar, então gravar depois do rollback
     * é seguro. Devolve o ACK {@code SKIPPED}.
     */
    SyncAckDTO recordSkipped(SyncOperationDTO operation);
}
