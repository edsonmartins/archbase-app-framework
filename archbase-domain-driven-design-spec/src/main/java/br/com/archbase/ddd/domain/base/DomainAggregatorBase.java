package br.com.archbase.ddd.domain.base;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Identifier;
import br.com.archbase.ddd.domain.contracts.SecurityUser;
import br.com.archbase.ddd.domain.contracts.ValidationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class DomainAggregatorBase<T extends AggregateRoot<T, Identifier>> implements AggregateRoot<T, Identifier> {

    @Schema(description = "Identificador único da entidade")
    protected Identifier id;

    @Schema(description = "Código da entidade")
    protected String code;

    @Schema(description = "Versão da entidade")
    protected Long version;

    @Schema(description = "Data de criação da entidade")
    protected LocalDateTime createEntityDate;

    @Schema(description = "Usuário que criou a entidade")
    protected String createdByUser;

    @Schema(description = "Data de atualização da entidade")
    protected LocalDateTime updateEntityDate;

    @Schema(description = "Usuário que modificou a entidade pela última vez")
    protected String lastModifiedByUser;

    public abstract ValidationResult validate();

    public DomainAggregatorBase() {
        this.id = new ArchbaseIdentifier();
        this.version = 1L;
        this.createEntityDate = LocalDateTime.now();
    }

    public DomainAggregatorBase(String id, String code) {
        this();
        this.id = new ArchbaseIdentifier(id);
        this.code = code;
    }

    public DomainAggregatorBase(String code) {
        this();
        this.code = code;
    }

    public DomainAggregatorBase(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser) {
        this();
        this.id = new ArchbaseIdentifier(id);
        this.code = code;
        if (version != null) {
            this.version = version;
        }
        this.createEntityDate = createEntityDate != null ? createEntityDate : this.createEntityDate;
        this.updateEntityDate = updateEntityDate != null ? updateEntityDate : this.updateEntityDate;
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
        if (this.createdByUser == null){
            this.createdByUser = lastModifiedByUser;
            this.createEntityDate = updateEntityDate;
        }
    }
}

