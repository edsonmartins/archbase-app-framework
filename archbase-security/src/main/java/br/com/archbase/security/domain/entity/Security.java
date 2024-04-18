package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.base.DomainAggregatorBase;
import br.com.archbase.ddd.domain.contracts.Identifier;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
public abstract class Security<T extends AggregateRoot<T, Identifier>, E> extends DomainAggregatorBase<T> {

    protected String name;
    protected String description;
    protected AccessSchedule accessSchedule;
    protected String email;

    public Security(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, AccessSchedule accessSchedule, String email) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.name = name;
        this.description = description;
        this.accessSchedule = accessSchedule;
        this.email = email;
    }

}

