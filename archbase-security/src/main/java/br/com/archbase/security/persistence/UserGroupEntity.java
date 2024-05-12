package br.com.archbase.security.persistence;


import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.UserGroupDto;
import br.com.archbase.security.domain.entity.UserGroup;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="SEGURANCA_GRUPO_USUARIO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_USUARIO_GRUPO")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_USUARIO_GRUPO"))
})
public class UserGroupEntity extends TenantPersistenceEntityBase {

    @ManyToOne
    @JoinColumn(name = "ID_USUARIO")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "ID_GRUPO")
    private GroupEntity group;

    public UserGroupEntity() {
    }

    @Builder
    public UserGroupEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, UserEntity user, GroupEntity group) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.user = user;
        this.group = group;
    }

    public static UserGroupEntity fromDomain(UserGroup userGroup, UserEntity owner) {
        if (userGroup == null) {
            return null;
        }

        GroupEntity groupEntity = GroupEntity.fromDomain(userGroup.getGroup());

        return UserGroupEntity.builder()
                .id(userGroup.getId().toString())
                .code(userGroup.getCode())
                .version(userGroup.getVersion())
                .updateEntityDate(userGroup.getUpdateEntityDate())
                .createEntityDate(userGroup.getCreateEntityDate())
                .createdByUser(userGroup.getCreatedByUser())
                .lastModifiedByUser(userGroup.getLastModifiedByUser())
                .user(owner)
                .group(groupEntity)
                .build();
    }

    public UserGroup toDomain() {
        return UserGroup.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .group(this.group != null ? this.group.toDomain() : null)
                .build();
    }

    public UserGroupDto toDto() {
        return UserGroupDto.builder()
                .id(this.id)
                .code(this.code)
                .version(this.version)
                .updateEntityDate(this.updateEntityDate)
                .createEntityDate(this.createEntityDate)
                .createdByUser(this.createdByUser)
                .lastModifiedByUser(this.lastModifiedByUser)
                .group(this.group != null ? this.group.toDto() : null)
                .build();
    }
}
