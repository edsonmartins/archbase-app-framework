package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.Action;
import br.com.archbase.security.domain.entity.Permission;
import br.com.archbase.security.domain.entity.Profile;
import br.com.archbase.security.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class PermissionDto  {

    protected String id;
    protected String code;
    protected Long version;
    protected LocalDateTime createEntityDate;
    protected LocalDateTime updateEntityDate;
    protected String createdByUser;
    protected String lastModifiedByUser;
    protected UserDto user;
    protected ActionDto action;
    protected String tenantId;
    protected String companyId;
    protected String projectId;

    public PermissionDto() {
    }

    @Builder
    public PermissionDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, UserDto user, ActionDto action, String tenantId, String companyId, String projectId) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.createEntityDate = createEntityDate;
        this.updateEntityDate = updateEntityDate;
        this.createdByUser = createdByUser;
        this.lastModifiedByUser = lastModifiedByUser;
        this.user = user;
        this.action = action;
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.projectId = projectId;
    }

    public static PermissionDto fromDomain(Permission permission) {
        if (permission == null) {
            return null;
        }

        return PermissionDto.builder()
                .id(permission.getId().toString())
                .code(permission.getCode())
                .version(permission.getVersion())
                .createEntityDate(permission.getCreateEntityDate())
                .updateEntityDate(permission.getUpdateEntityDate())
                .createdByUser(permission.getCreatedByUser())
                .lastModifiedByUser(permission.getLastModifiedByUser())
                .user(UserDto.fromDomain(permission.getUser()))
                .action(ActionDto.fromDomain(permission.getAction()))
                .tenantId(permission.getTenantId())
                .companyId(permission.getCompanyId())
                .projectId(permission.getProjectId())
                .build();
    }

    public Permission toDomain() {
        return Permission.builder()
                .id(this.id)
                .code(this.code)
                .version(this.version)
                .createEntityDate(this.createEntityDate)
                .updateEntityDate(this.updateEntityDate)
                .createdByUser(this.createdByUser)
                .lastModifiedByUser(this.lastModifiedByUser)
                .tenantId(this.tenantId)
                .companyId(this.companyId)
                .projectId(this.projectId)
                .user(this.user != null ? this.user.toDomain() : null)
                .action(this.action != null ? this.action.toDomain() : null)
                .build();
    }
}
