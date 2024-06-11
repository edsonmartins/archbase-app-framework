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


    public GroupEntity() {
        super();
    }

    @Builder
    public GroupEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId, name, description);
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
                .build();
    }
}
