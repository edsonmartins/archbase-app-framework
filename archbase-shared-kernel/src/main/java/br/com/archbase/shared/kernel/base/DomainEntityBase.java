package br.com.archbase.shared.kernel.base;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Entity;
import br.com.archbase.shared.kernel.identifier.ArchbaseIdentifier;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class DomainEntityBase<T extends AggregateRoot<T, ?>> implements Entity<T, ArchbaseIdentifier> {

    protected final ArchbaseIdentifier id;

    protected String code;
    protected LocalDateTime dhInsert;
    protected LocalDateTime dhUpdate;

    protected Long version;

    public DomainEntityBase() {
        this(new ArchbaseIdentifier());
    }

    public DomainEntityBase(ArchbaseIdentifier id) {
       this.id = id;
       this.version = 1L;
       this.dhInsert = LocalDateTime.now();
       this.dhUpdate = LocalDateTime.now();
    }

    public DomainEntityBase(ArchbaseIdentifier id, String code) {
        this.id = id;
        this.version = 1L;
        this.dhInsert = LocalDateTime.now();
        this.dhUpdate = LocalDateTime.now();
        this.code = code;
    }

    @Override
    public ArchbaseIdentifier getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEntityBase<?> that = (DomainEntityBase<?>) o;
        return Objects.equals(id, that.id) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getDhInsert() {
        return dhInsert;
    }

    public LocalDateTime getDhUpdate() {
        return dhUpdate;
    }

    public Long getVersion() {
        return version;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public abstract void validate();
}