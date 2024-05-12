package br.com.archbase.security.domain.entity;


import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.base.DomainEntityBase;
import br.com.archbase.ddd.domain.contracts.ValidationResult;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@DomainEntity
public class UserGroup extends DomainEntityBase<User> {

    protected Group group;

    @Builder
    public UserGroup(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, Group group) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.group = group;
    }

    static class Validator extends AbstractArchbaseValidator<UserGroup> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new UserGroup.Validator().validate(this);
    }
}
