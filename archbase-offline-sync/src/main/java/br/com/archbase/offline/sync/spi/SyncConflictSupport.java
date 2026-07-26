package br.com.archbase.offline.sync.spi;

import br.com.archbase.offline.sync.exception.SyncConflictException;
import br.com.archbase.offline.sync.exception.SyncSkippedException;

import java.time.LocalDateTime;

/**
 * Helpers para os handlers aplicarem a semântica de conflito de forma uniforme
 * (as três regras do contrato) — evita cada entidade reinventar (e divergir).
 */
public final class SyncConflictSupport {

    private SyncConflictSupport() {
    }

    /**
     * No-resurrection: se o registro já foi apagado no servidor, um upsert
     * atrasado é no-op (não ressuscita). Lança {@link SyncSkippedException}.
     */
    public static void checkNotDeleted(String entityType, String entityId,
                                       LocalDateTime storedDeletedAt) {
        if (storedDeletedAt != null) {
            throw new SyncSkippedException(
                    entityType + " " + entityId + " já apagado; upsert ignorado");
        }
    }

    /**
     * Conflito por versão otimista: se o cliente baseou-se numa versão diferente
     * da atual do servidor, é CONFLICT.
     */
    public static void checkVersion(String entityType, String entityId,
                                    Long baseVersion, Long serverVersion) {
        if (baseVersion != null && serverVersion != null
                && !baseVersion.equals(serverVersion)) {
            throw new SyncConflictException(entityType, entityId, baseVersion, serverVersion);
        }
    }

    /**
     * LWW estrito: aplica só se a escrita do cliente for ESTRITAMENTE mais nova
     * que a última conhecida; senão é no-op idempotente (SKIPPED), sem sobrescrever.
     */
    public static void checkLww(String entityType, String entityId,
                                LocalDateTime incoming, LocalDateTime stored) {
        if (stored != null && (incoming == null || !incoming.isAfter(stored))) {
            throw new SyncSkippedException(
                    entityType + " " + entityId + " ignorado (LWW: não é mais novo)");
        }
    }
}
