package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.base.DomainAggregatorBase;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@DomainEntity
public class Resource extends DomainAggregatorBase<Resource> {

    protected String name;
    protected String description;
    protected Boolean active;

    @Builder
    public Resource(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, Boolean active) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.name = name;
        this.description = description;
        this.active = active;
    }

    static class Validator extends AbstractArchbaseValidator<Resource> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}

