package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.entity.Group;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("SEGURANCA_GRUPO")
@Getter
public class GroupEntity extends SecurityEntity {

    @ManyToMany
    @JoinTable(
            name = "SEGURANCA_GRUPO_USUARIO",
            joinColumns = @JoinColumn(name = "ID_GRUPO"),
            inverseJoinColumns = @JoinColumn(name = "ID_USUARIO")
    )
    private List<UserEntity> members;

    public GroupEntity() {
        super();
    }

    @Builder
    public GroupEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, Set<ActionEntity> actions, String email, List<UserEntity> members) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId, name, description, email);
        this.members = members;
    }

    public static GroupEntity fromDomain(Group group) {
        if (group == null) {
            return null;
        }

        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(group.getId().toString());
        groupEntity.setCode(group.getCode());
        groupEntity.setVersion(group.getVersion());
        groupEntity.setCreateEntityDate(group.getCreateEntityDate());
        groupEntity.setUpdateEntityDate(group.getUpdateEntityDate());
        groupEntity.setCreatedByUser(group.getCreatedByUser());
        groupEntity.setLastModifiedByUser(group.getLastModifiedByUser());
        groupEntity.setName(group.getName());
        groupEntity.setDescription(group.getDescription());

        groupEntity.setEmail(group.getEmail());

        groupEntity.members = group.getMembers() != null ?
                group.getMembers().stream().map(UserEntity::fromDomain).collect(Collectors.toList()) :
                new ArrayList<>();

        return groupEntity;
    }

    public Group toDomain() {
        return Group.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .email(this.getEmail())
                .members(this.members != null ? this.members.stream().map(UserEntity::toDomain).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    public GroupDto toDto() {
        return GroupDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .email(this.getEmail())
                .members(this.members != null ? this.members.stream().map(UserEntity::toDto).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }
}
