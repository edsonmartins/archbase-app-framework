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
public class AccessInterval extends DomainEntityBase<AccessSchedule> {

    protected AccessSchedule accessSchedule;
    protected Long dayOfWeek;
    protected String startTime;
    protected String endTime;

    @Builder
    public AccessInterval(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, AccessSchedule accessSchedule, Long dayOfWeek, String startTime, String endTime) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.accessSchedule = accessSchedule;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    static class Validator extends AbstractArchbaseValidator<AccessInterval> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}
