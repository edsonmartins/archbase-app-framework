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

    @Column(name = "COMPANY_ID", length = 40)
    private String companyId;

    public CompanyPersistenceEntityBase() {
        this.id = UUID.randomUUID().toString();
        this.version = 1L;
        this.updateEntityDate = LocalDateTime.now();
        this.createEntityDate = LocalDateTime.now();
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

    public CompanyPersistenceEntityBase(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.createEntityDate = createEntityDate;
        this.updateEntityDate = updateEntityDate;
        this.createdByUser = createdByUser;
        this.lastModifiedByUser = lastModifiedByUser;
    }
}