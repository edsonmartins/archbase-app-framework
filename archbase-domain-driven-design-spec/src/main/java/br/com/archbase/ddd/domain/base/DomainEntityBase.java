package br.com.archbase.ddd.domain.base;


import br.com.archbase.ddd.domain.contracts.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public abstract class DomainEntityBase<T extends AggregateRoot<T, Identifier>> implements Entity<T, Identifier> {

    protected ArchbaseIdentifier id;

    protected String code;

    protected Long version;

    protected LocalDateTime createEntityDate;

    protected LocalDateTime updateEntityDate;

    protected String createdByUser;

    protected String lastModifiedByUser;

    public abstract ValidationResult validate();

    public DomainEntityBase() {
        this.id = new ArchbaseIdentifier();
        this.version = 1L;
        this.createEntityDate = LocalDateTime.now();
        this.updateEntityDate = LocalDateTime.now();
    }

    public DomainEntityBase(String id, String code) {
        this();
        this.id = new ArchbaseIdentifier(id);
        this.code = code;
    }

    public DomainEntityBase(String code) {
        this();
        this.code = code;
    }

    public DomainEntityBase(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser) {
        this();
        this.id = new ArchbaseIdentifier(id);
        this.code = code;
        if (version != null) {
            this.version = version;
        }
        this.createEntityDate = createEntityDate;
        this.updateEntityDate = updateEntityDate;
        this.createdByUser = createdByUser;
        this.lastModifiedByUser = lastModifiedByUser;
    }

    public void createdBy(SecurityUser user) {
        this.createdByUser = user.getUserName();
        this.lastModifiedByUser = user.getUserName();
        this.updateEntityDate = LocalDateTime.now();
        this.createEntityDate = LocalDateTime.now();
    }

    public void modifiedBy(SecurityUser user) {
        this.lastModifiedByUser = user.getUserName();
        this.updateEntityDate = LocalDateTime.now();
        if (this.createdByUser == null) {
            this.createdByUser = lastModifiedByUser;
            this.createEntityDate = updateEntityDate;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEntityBase<?> that = (DomainEntityBase<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
