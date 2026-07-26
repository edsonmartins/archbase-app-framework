package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncAckDTO;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;

/**
 * Executa UMA operação em transação própria. Porta explícita para (a) garantir
 * que a chamada passe pelo proxy Spring (REQUIRES_NEW) e (b) permitir fake em teste.
 */
public interface SyncOperationExecutorPort {

    /**
     * Aplica a operação numa transação isolada. Deve lançar em erro transitório
     * (para reverter só esta operação); conflito/skip/rejeição voltam como ACK.
     */
    SyncAckDTO execute(SyncOperationDTO operation);
}
