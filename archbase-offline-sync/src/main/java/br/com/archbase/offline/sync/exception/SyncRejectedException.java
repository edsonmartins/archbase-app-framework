package br.com.archbase.offline.sync.exception;

/**
 * Erro de negócio TERMINAL ao aplicar a operação (&rarr; REJECTED).
 *
 * <p>Diferente de uma {@link RuntimeException} genérica (que vira FAILED,
 * transitória, e o cliente reenvia com backoff), um REJECTED sinaliza que a
 * operação NÃO deve ser retentada às cegas — ex.: a entidade-âncora não existe
 * mais ("Visita não encontrada"), payload inválido, regra de negócio violada.
 * O cliente marca o registro em erro (com o motivo) e oferece descartar/tentar
 * de novo ao usuário.
 *
 * <p><b>Uso:</b> lance ANTES de qualquer escrita transacional do handler — assim
 * como {@code SyncConflictException} —, senão a transação {@code REQUIRES_NEW}
 * fica marcada como rollback-only e o ACK viraria FAILED em vez de REJECTED.
 */
public class SyncRejectedException extends RuntimeException {
    public SyncRejectedException(String message) {
        super(message);
    }
}
