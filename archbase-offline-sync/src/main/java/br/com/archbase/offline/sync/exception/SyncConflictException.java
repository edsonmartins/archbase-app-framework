package br.com.archbase.offline.sync.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/** Conflito de estado/versão ao aplicar a operação (→ CONFLICT). */
public class SyncConflictException extends RuntimeException {
    private final Map<String, Object> detail = new LinkedHashMap<>();

    public SyncConflictException(String message) {
        super(message);
    }

    public SyncConflictException(String entityType, String entityId,
                                 Long baseVersion, Long serverVersion) {
        super("Conflito em " + entityType + " " + entityId
                + " (base=" + baseVersion + ", server=" + serverVersion + ")");
        detail.put("entityType", entityType);
        detail.put("entityId", entityId);
        detail.put("baseVersion", baseVersion);
        detail.put("serverVersion", serverVersion);
    }

    public Map<String, Object> getDetail() {
        return detail;
    }
}
