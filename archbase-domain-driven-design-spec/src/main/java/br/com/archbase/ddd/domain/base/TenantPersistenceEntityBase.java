package br.com.archbase.ddd.domain.base;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public class TenantPersistenceEntityBase {

    @Id
    @Column(name="ID")
    protected String id;

    @Column(name="CODE")
    protected String code;

    @Version
    @Column(name="VERSION")
    protected Long version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="CREATION_DATE")
    protected LocalDateTime createEntityDate;

    @Column(name="CREATED_BY")
    protected String createdByUser;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="UPDATE_DATE")
    protected LocalDateTime updateEntityDate;

    @Column(name="LAST_MODIFIED_BY")
    protected String lastModifiedByUser;

    @TenantId
    @Column(name = "TENANT_ID", length = 40)
    private String tenantId;

    public TenantPersistenceEntityBase() {
        this.id = UUID.randomUUID().toString();
        this.version = 1L;
        this.createEntityDate = LocalDateTime.now();
        this.updateEntityDate = LocalDateTime.now();
    }

    public TenantPersistenceEntityBase(String id, String code) {
        this();
        if (id != null) {
            this.id = id;
        }
        this.code = code;
    }

    public TenantPersistenceEntityBase(String code) {
        this();
        this.code = code;
    }

    public TenantPersistenceEntityBase(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.createEntityDate = createEntityDate;
        this.createdByUser = createdByUser;
        this.updateEntityDate = updateEntityDate;
        this.lastModifiedByUser = lastModifiedByUser;
        this.tenantId = tenantId;
    }
}
