package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.base.DomainAggregatorBase;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@DomainEntity
public class Permission extends DomainAggregatorBase<Resource> {
    private Security security;
    private Action action;
    private String tenantId;
    private String companyId;
    private String projectId;

    @Builder
    public Permission(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, Security security, Action action, String tenantId, String companyId, String projectId) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.security = security;
        this.action = action;
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.projectId = projectId;
    }

    static class Validator extends AbstractArchbaseValidator<Permission> {
        @Override
        public void rules() {
            // Add validation rules here
        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}
