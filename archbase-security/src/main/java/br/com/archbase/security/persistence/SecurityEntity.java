package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="SEGURANCA")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="TP_SEGURANCA")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_SEGURANCA")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_SEGURANCA"))
})
public abstract class SecurityEntity extends TenantPersistenceEntityBase {
    @NotBlank
    @Column(name = "NOME", nullable = false)
    private String name;
    @NotBlank
    @Column(name = "DESCRICAO", nullable = false)
    private String description;

    public SecurityEntity() {
        // Default empty constructor
    }

    public SecurityEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.name = name;
        this.description = description;
    }
}
