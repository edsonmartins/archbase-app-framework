package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.entity.Action;
import br.com.archbase.security.domain.entity.Resource;
import br.com.archbase.shared.kernel.converters.BooleanToSNConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name="SEGURANCA_ACAO")
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_ACAO")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_ACAO"))
})
public class ActionEntity extends TenantPersistenceEntityBase {

    @Column(name = "NOME", nullable = false)
    private String name;

    @Column(name = "DESCRICAO", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "ID_RECURSO", nullable = false)
    private ResourceEntity resource;

    @Column(name = "CATEGORIA", nullable = true)
    private String category;

    @Column(name = "BO_ATIVA", nullable = false, length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean active;

    @Column(name = "VERSAO_ACAO", nullable = true)
    private String actionVersion;

    public ActionEntity() {
        super();
    }

    @Builder
    public ActionEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, ResourceEntity resource, String category, Boolean active, String actionVersion) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.category = category;
        this.active = active;
        this.actionVersion = actionVersion;
    }

    public static ActionEntity fromDomain(Action action) {
        if (action == null) {
            return null;
        }

        ActionEntity actionEntity = new ActionEntity();
        actionEntity.setId(action.getId().toString());
        actionEntity.setCode(action.getCode());
        actionEntity.setVersion(action.getVersion());
        actionEntity.setName(action.getName());
        actionEntity.setDescription(action.getDescription());
        actionEntity.setResource(ResourceEntity.fromDomain(action.getResource()));
        actionEntity.setCategory(action.getCategory());
        actionEntity.setActive(action.getActive());
        actionEntity.setActionVersion(action.getActionVersion());
        return actionEntity;
    }

    public Action toDomain() {
        Resource resourceDomain = this.resource != null ? this.resource.toDomain() : null;

        return Action.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .resource(resourceDomain)
                .category(this.getCategory())
                .active(this.getActive())
                .actionVersion(this.getActionVersion())
                .build();
    }

    public ActionDto toDto() {
        ResourceDto resourceDto = this.resource != null ? this.resource.toDto() : null;

        return ActionDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .resource(resourceDto)
                .category(this.getCategory())
                .active(this.getActive())
                .actionVersion(this.getActionVersion())
                .build();
    }
}
