package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
@DomainEntity
public class Group extends Security<Group, User> {
    @Builder
    public Group(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, AccessSchedule accessSchedule, String email) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser, name, description, accessSchedule, email);
    }


    static class Validator extends AbstractArchbaseValidator<Group> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}
