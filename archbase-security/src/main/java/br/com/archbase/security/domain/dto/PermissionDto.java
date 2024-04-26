package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.*;
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
    protected SecurityDto security;
    protected ActionDto action;
    protected String tenantId;
    protected String companyId;
    protected String projectId;

    public PermissionDto() {
    }

    @Builder
    public PermissionDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, SecurityDto security, ActionDto action, String tenantId, String companyId, String projectId) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.createEntityDate = createEntityDate;
        this.updateEntityDate = updateEntityDate;
        this.createdByUser = createdByUser;
        this.lastModifiedByUser = lastModifiedByUser;
        this.security = security;
        this.action = action;
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.projectId = projectId;
    }

    public static PermissionDto fromDomain(Permission permission) {
        if (permission == null) {
            return null;
        }

        SecurityDto securityDto = null;
        if (permission.getSecurity() != null) {
            if (permission.getSecurity() instanceof User) {
                securityDto = UserDto.fromDomain((User) permission.getSecurity());
            } else if (permission.getSecurity() instanceof Group){
                securityDto = GroupDto.fromDomain((Group) permission.getSecurity());
            } else if (permission.getSecurity() instanceof Profile) {
                securityDto = ProfileDto.fromDomain((Profile) permission.getSecurity());
            }
        }


        return PermissionDto.builder()
                .id(permission.getId().toString())
                .code(permission.getCode())
                .version(permission.getVersion())
                .createEntityDate(permission.getCreateEntityDate())
                .updateEntityDate(permission.getUpdateEntityDate())
                .createdByUser(permission.getCreatedByUser())
                .lastModifiedByUser(permission.getLastModifiedByUser())
                .security(securityDto)
                .action(ActionDto.fromDomain(permission.getAction()))
                .tenantId(permission.getTenantId())
                .companyId(permission.getCompanyId())
                .projectId(permission.getProjectId())
                .build();
    }

    public Permission toDomain() {
        Security<?,?> security = null;
        if (this.security != null) {
            if (this.security instanceof UserDto) {
                security = ((UserDto) this.security).toDomain();
            } else if (this.security instanceof GroupDto){
                security = ((GroupDto) this.security).toDomain();
            } else if (this.security instanceof ProfileDto) {
                security = ((ProfileDto) this.security).toDomain();
            }
        }


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
                .security(security)
                .action(this.action != null ? this.action.toDomain() : null)
                .build();
    }
}
