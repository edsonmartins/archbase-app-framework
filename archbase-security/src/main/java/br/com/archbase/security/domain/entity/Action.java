package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.base.DomainEntityBase;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@DomainEntity
public class Action extends DomainEntityBase<Resource> {

    protected String name;
    protected String description;
    protected Resource resource;
    protected String category;
    protected Boolean active;
    protected String actionVersion;

    @Builder
    public Action(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, Resource resource, String category, Boolean active, String actionVersion) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.category = category;
        this.active = active;
        this.actionVersion = actionVersion;
    }

    static class Validator extends AbstractArchbaseValidator<Action> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}
