package br.com.archbase.ddd.domain.base;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class CompanyEntityListener {

    @PrePersist
    public void prePersist(CompanyPersistenceEntityBase entity) {
        entity.setCompanyId(ArchbaseTenantContext.getCompanyId());
    }

    @PreRemove
    public void preRemove(CompanyPersistenceEntityBase entity) {
        entity.setCompanyId(ArchbaseTenantContext.getCompanyId());
    }

    @PreUpdate
    public void preUpdate(CompanyPersistenceEntityBase entity) {
        entity.setCompanyId(ArchbaseTenantContext.getCompanyId());
    }
}
