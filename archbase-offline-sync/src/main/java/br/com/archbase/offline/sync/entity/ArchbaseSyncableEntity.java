package br.com.archbase.offline.sync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Base opcional para entidades sincronizáveis (offline-first).
 *
 * <p>Adiciona as colunas de sync: {@code version} (lock otimista),
 * {@code client_updated_at} (relógio do cliente para LWW) e {@code deleted_at}
 * (soft delete). Auto-contida (só jakarta.persistence) — o app pode usar esta
 * base OU adicionar as mesmas colunas à sua base já existente.
 *
 * <p>Para o filtro global de soft delete, a entidade concreta deve anotar
 * {@code @SQLRestriction("deleted_at is null")} (ou {@code @SoftDelete}) — não é
 * imposto aqui para não acoplar o módulo ao Hibernate.
 */
@MappedSuperclass
public abstract class ArchbaseSyncableEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Version
    @Column(name = "version")
    private Long version;

    /** Última escrita no cliente (LWW). */
    @Column(name = "client_updated_at")
    private LocalDateTime clientUpdatedAt;

    /** Soft delete: preenchido = apagado (não deve ressuscitar). */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected ArchbaseSyncableEntity() {
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(LocalDateTime when) {
        this.deletedAt = when != null ? when : LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getClientUpdatedAt() { return clientUpdatedAt; }
    public void setClientUpdatedAt(LocalDateTime v) { this.clientUpdatedAt = v; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime v) { this.deletedAt = v; }
}
