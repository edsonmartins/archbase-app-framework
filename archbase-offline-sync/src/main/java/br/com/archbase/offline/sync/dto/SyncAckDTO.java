package br.com.archbase.offline.sync.dto;

import java.util.Map;

/** ACK de uma operação, devolvido ao cliente. */
public class SyncAckDTO {
    public String id;
    public SyncOpStatus status;
    public Long serverVersion;
    public String error;
    public String dependsOn;
    /** Detalhe do conflito (entityType/entityId/baseVersion/serverVersion). */
    public Map<String, Object> conflict;

    public SyncAckDTO() {
    }

    private SyncAckDTO(String id, SyncOpStatus status) {
        this.id = id;
        this.status = status;
    }

    public static SyncAckDTO processed(String id, Long serverVersion) {
        SyncAckDTO a = new SyncAckDTO(id, SyncOpStatus.PROCESSED);
        a.serverVersion = serverVersion;
        return a;
    }

    public static SyncAckDTO skipped(String id) {
        return new SyncAckDTO(id, SyncOpStatus.SKIPPED);
    }

    public static SyncAckDTO conflict(String id, Map<String, Object> conflict) {
        SyncAckDTO a = new SyncAckDTO(id, SyncOpStatus.CONFLICT);
        a.conflict = conflict;
        return a;
    }

    public static SyncAckDTO blocked(String id, String dependsOn) {
        SyncAckDTO a = new SyncAckDTO(id, SyncOpStatus.BLOCKED);
        a.dependsOn = dependsOn;
        return a;
    }

    public static SyncAckDTO rejected(String id, String error) {
        SyncAckDTO a = new SyncAckDTO(id, SyncOpStatus.REJECTED);
        a.error = error;
        return a;
    }

    public static SyncAckDTO failed(String id, String error) {
        SyncAckDTO a = new SyncAckDTO(id, SyncOpStatus.FAILED);
        a.error = error;
        return a;
    }
}
