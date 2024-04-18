package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainAggregateRoot;
import br.com.archbase.ddd.domain.contracts.ValidationResult;
import br.com.archbase.ddd.domain.base.DomainAggregatorBase;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@DomainAggregateRoot
public class AccessSchedule extends DomainAggregatorBase<AccessSchedule> {

    protected String description;
    protected List<AccessInterval> intervals = new ArrayList<>();

    @Builder
    public AccessSchedule(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String description, List<AccessInterval> intervals) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.description = description;
        this.intervals = intervals;
    }

    public List<AccessInterval> getIntervals() {
        return Collections.unmodifiableList(intervals);
    }

    @Override
    public ValidationResult validate() {
        return new Validator().validate(this);
    }

    static class Validator extends AbstractArchbaseValidator<AccessSchedule> {
        @Override
        public void rules() {

        }
    }
}

