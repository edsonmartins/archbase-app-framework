package br.com.archbase.offline.sync.spi;

import br.com.archbase.offline.sync.dto.SyncOperationDTO;

/**
 * Handler de um tipo de operação de sync. Cada app registra um bean por tipo
 * (ex.: {@code VISIT_CHECK_IN}); o framework descobre e despacha.
 *
 * <p>O handler é chamado DENTRO de uma transação própria (REQUIRES_NEW): uma
 * falha aqui reverte só esta operação, nunca o lote inteiro.
 *
 * <p>Sinalização de resultado:
 * <ul>
 *   <li>retorno normal → {@code PROCESSED} (com {@code serverVersion});</li>
 *   <li>lançar {@link br.com.archbase.offline.sync.exception.SyncSkippedException}
 *       → {@code SKIPPED} (no-op idempotente, ex.: estado terminal já atingido);</li>
 *   <li>lançar {@link br.com.archbase.offline.sync.exception.SyncConflictException}
 *       (ou {@code SyncVersionConflictException}) → {@code CONFLICT};</li>
 *   <li>qualquer outra exceção → {@code FAILED} (transitório; o cliente reenvia).</li>
 * </ul>
 */
public interface SyncOperationHandler {

    /** Tipo tratado (ex.: {@code "VISIT_CHECK_IN"}). */
    String type();

    /** Aplica a operação e devolve o novo {@code serverVersion} (pode ser null). */
    SyncHandlerResult handle(SyncOperationDTO operation);
}
