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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name="SEGURANCA_RECURSO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_RECURSO")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_RECURSO"))
})
public class ResourceEntity extends TenantPersistenceEntityBase {

    @Column(name = "NOME", nullable = false)
    private String name;

    @Column(name = "DESCRICAO", nullable = false)
    private String description;

    @Column(name = "BO_ATIVO", nullable = false, length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean active;

    @ManyToMany
    @JoinTable(name = "SEGURANCA_RECURSO_ACAO",
            joinColumns = @JoinColumn(name = "ID_RECURSO"),
            inverseJoinColumns = @JoinColumn(name = "ID_ACAO"))
    private List<ActionEntity> actions;


    public ResourceEntity() {
        // Default empty constructor
    }

    @Builder
    public ResourceEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, List<ActionEntity> actions) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.name = name;
        this.description = description;
        this.actions = actions;
    }


    public static ResourceEntity fromDomain(Resource resource) {
        if (resource == null) {
            return null;
        }

        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setId(resource.getId().toString());
        resourceEntity.setCode(resource.getCode());
        resourceEntity.setVersion(resource.getVersion());
        resourceEntity.setUpdateEntityDate(resource.getUpdateEntityDate());
        resourceEntity.setCreateEntityDate(resource.getCreateEntityDate());
        resourceEntity.setCreatedByUser(resource.getCreatedByUser());
        resourceEntity.setLastModifiedByUser(resource.getLastModifiedByUser());
        resourceEntity.setName(resource.getName());
        resourceEntity.setDescription(resource.getDescription());
        resourceEntity.setActions(resource.getActions() != null ?
                resource.getActions().stream().map(ActionEntity::fromDomain).collect(Collectors.toList()) : null);
        return resourceEntity;
    }

    public Resource toDomain() {
        List<Action> actionsDomain = this.actions != null
                ? this.actions.stream().map(ActionEntity::toDomain).collect(Collectors.toList())
                : Collections.emptyList();

        return Resource.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .actions(actionsDomain)
                .build();
    }

    public ResourceDto toDto() {
        List<ActionDto> actionsDto = this.actions != null
                ? this.actions.stream().map(ActionEntity::toDto).collect(Collectors.toList())
                : Collections.emptyList();


        return ResourceDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .actions(actionsDto)
                .build();
    }
}
