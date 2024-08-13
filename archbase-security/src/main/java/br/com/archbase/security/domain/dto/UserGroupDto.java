package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.UserGroup;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class UserGroupDto {

    protected String id;
    protected String code;
    protected Long version;
    protected LocalDateTime createEntityDate;
    protected LocalDateTime updateEntityDate;
    protected String createdByUser;
    protected String lastModifiedByUser;
    protected GroupDto group;

    @Builder
    public UserGroupDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, GroupDto group) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.createEntityDate = createEntityDate;
        this.updateEntityDate = updateEntityDate;
        this.createdByUser = createdByUser;
        this.lastModifiedByUser = lastModifiedByUser;
        this.group = group;
    }

    public static UserGroupDto fromDomain(UserGroup userGroup) {
        if (userGroup == null) {
            return null;
        }

        return UserGroupDto.builder()
                .id(userGroup.getId().toString())
                .code(userGroup.getCode())
                .version(userGroup.getVersion())
                .createEntityDate(userGroup.getCreateEntityDate())
                .updateEntityDate(userGroup.getUpdateEntityDate())
                .createdByUser(userGroup.getCreatedByUser())
                .lastModifiedByUser(userGroup.getLastModifiedByUser())
                .group(GroupDto.fromDomain(userGroup.getGroup()))
                .build();
    }

    public UserGroup toDomain() {
        return UserGroup.builder()
                .id(this.id)
                .code(this.code)
                .version(this.version)
                .createEntityDate(this.createEntityDate)
                .updateEntityDate(this.updateEntityDate)
                .createdByUser(this.createdByUser)
                .lastModifiedByUser(this.lastModifiedByUser)
                .group(this.group != null ?this.group.toDomain():null)
                .build();
    }
}
