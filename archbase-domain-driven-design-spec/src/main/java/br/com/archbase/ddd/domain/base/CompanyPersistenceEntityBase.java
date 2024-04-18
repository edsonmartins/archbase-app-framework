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
@EntityListeners(CompanyEntityListener.class)
@FilterDef(name = "companyFilter", parameters = @ParamDef(name = "companyId", type = String.class))
@Filters({
        @Filter(name = "companyFilter", condition = "COMPANY_ID = :companyId")
})
public class CompanyPersistenceEntityBase {

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
    protected LocalDateTime creationDateTime;

    @Column(name="CREATED_BY")
    protected String createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="UPDATE_DATE")
    protected LocalDateTime updateDateTime;

    @Column(name="LAST_MODIFIED_BY")
    protected String lastModifiedBy;

    @TenantId
    @Column(name = "TENANT_ID", length = 40)
    private String tenantId;

    @Column(name = "COMPANY_ID", length = 40)
    private String companyId;

    public CompanyPersistenceEntityBase() {
        this.id = UUID.randomUUID().toString();
        this.version = 1L;
        this.updateDateTime = LocalDateTime.now();
        this.creationDateTime = LocalDateTime.now();
    }

    public CompanyPersistenceEntityBase(String id, String code) {
        this();
        if (id != null) {
            this.id = id;
        }
        this.code = code;
    }

    public CompanyPersistenceEntityBase(String code) {
        this();
        this.code = code;
    }

    public CompanyPersistenceEntityBase(String id, String code, Long version, LocalDateTime creationDateTime, LocalDateTime updateDateTime, String createdBy, String lastModifiedBy) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.creationDateTime = creationDateTime;
        this.updateDateTime = updateDateTime;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }
}