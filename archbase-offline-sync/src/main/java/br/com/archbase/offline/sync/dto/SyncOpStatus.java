package br.com.archbase.offline.sync.dto;

/**
 * Status de uma operação de sync no ACK (contrato batch). No fio vai em UPPERCASE.
 *
 * <p>Espelha o {@code ArchbaseSyncOpStatus} do cliente Flutter (archbase_flutter).
 */
public enum SyncOpStatus {
    /** Aplicada com sucesso. */
    PROCESSED,
    /** Idempotência: já processada antes; não reexecutada. */
    SKIPPED,
    /** Conflito (versão/estado) — exige resolução no cliente. */
    CONFLICT,
    /** Dependência não concluída no mesmo lote — não executada. */
    BLOCKED,
    /** Tipo não registrado — erro de contrato. */
    REJECTED,
    /** Erro transitório — o cliente reenvia com backoff. */
    FAILED
}
