package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainAggregateRoot;
import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.base.ArchbaseIdentifier;
import br.com.archbase.ddd.domain.contracts.Identifier;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@DomainAggregateRoot
public class Profile extends Security<Profile, Profile> implements AggregateRoot<Profile, Identifier> {

    @Builder
    public Profile(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, AccessSchedule accessSchedule) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser, name, description, accessSchedule);
    }

    static class Validator extends AbstractArchbaseValidator<Profile> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}

