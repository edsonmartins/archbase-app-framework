package br.com.archbase.offline.sync.exception;

/** Sinaliza no-op idempotente: a operação não precisa ser aplicada (→ SKIPPED). */
public class SyncSkippedException extends RuntimeException {
    public SyncSkippedException(String message) {
        super(message);
    }
}
