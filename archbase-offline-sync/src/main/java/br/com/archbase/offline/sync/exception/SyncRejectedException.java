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
 * <p><b>Uso:</b> pode ser lançada a qualquer momento do handler, inclusive DEPOIS
 * de escritas de domínio. A partir do offline-sync 3.1.2 a exceção PROPAGA para
 * fora da transação {@code REQUIRES_NEW} do executor (não é mais capturada e
 * retornada dentro dela); o {@code SyncOperationProcessor} a converte em ACK
 * REJECTED DEPOIS do rollback. Assim o efeito parcial do handler reverte junto e
 * não há {@code UnexpectedRollbackException} — some a antiga armadilha de virar
 * FAILED quando a transação já estava rollback-only.
 */
public class SyncRejectedException extends RuntimeException {
    public SyncRejectedException(String message) {
        super(message);
    }
}
