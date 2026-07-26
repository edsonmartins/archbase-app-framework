package br.com.archbase.offline.sync.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Idempotência durável: registra operações já processadas para deduplicar
 * reenvios que sobrevivem a restart/semanas offline. PK = (tenantId, operationId).
 *
 * <p>Retenção (90d) é aplicada por job agendado (ver {@code SyncOperationProcessor}).
 */
@Entity
@Table(name = "processed_sync_operation")
@IdClass(ProcessedSyncOperation.Pk.class)
public class ProcessedSyncOperation {

    @Id
    @Column(name = "tenant_id", length = 40, nullable = false)
    private String tenantId;

    @Id
    @Column(name = "operation_id", length = 64, nullable = false)
    private String operationId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "operation_type", length = 80)
    private String operationType;

    @Column(name = "aggregate_id", length = 64)
    private String aggregateId;

    @Column(name = "server_version")
    private Long serverVersion;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedSyncOperation() {
    }

    public ProcessedSyncOperation(String tenantId, String operationId, String status,
                                  String operationType, String aggregateId,
                                  Long serverVersion, LocalDateTime processedAt) {
        this.tenantId = tenantId;
        this.operationId = operationId;
        this.status = status;
        this.operationType = operationType;
        this.aggregateId = aggregateId;
        this.serverVersion = serverVersion;
        this.processedAt = processedAt;
    }

    public String getTenantId() { return tenantId; }
    public String getOperationId() { return operationId; }
    public String getStatus() { return status; }
    public String getOperationType() { return operationType; }
    public String getAggregateId() { return aggregateId; }
    public Long getServerVersion() { return serverVersion; }
    public LocalDateTime getProcessedAt() { return processedAt; }

    /** Chave composta. */
    public static class Pk implements Serializable {
        private String tenantId;
        private String operationId;

        public Pk() {
        }

        public Pk(String tenantId, String operationId) {
            this.tenantId = tenantId;
            this.operationId = operationId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk)) return false;
            Pk pk = (Pk) o;
            return Objects.equals(tenantId, pk.tenantId)
                    && Objects.equals(operationId, pk.operationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, operationId);
        }
    }
}
