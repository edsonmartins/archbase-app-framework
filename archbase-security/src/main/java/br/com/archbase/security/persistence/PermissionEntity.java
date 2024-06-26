package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.PermissionDto;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.domain.dto.SecurityDto;
import br.com.archbase.security.domain.entity.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name="SEGURANCA_PERMISSAO")
@AttributeOverrides({
        @AttributeOverride(name="id", column=@Column(name="ID_PERMISSAO")),
        @AttributeOverride(name="code", column=@Column(name="CD_PERMISSAO"))
})
public class PermissionEntity extends TenantPersistenceEntityBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SEGURANCA", nullable = false)
    private SecurityEntity security;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ACAO", nullable = false)
    private ActionEntity action;
    @Column(name="TENTANT_ID", nullable = true)
    private String tenantId;
    @Column(name="COMPANY_ID", nullable = true)
    private String companyId;
    @Column(name="PROJECT_ID", nullable = true)
    private String projectId;

    public PermissionEntity() {
        super();
    }

    @Builder
    public PermissionEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, SecurityEntity security, ActionEntity action, String tenantId1, String companyId, String projectId) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.security = security;
        this.action = action;
        this.tenantId = tenantId1;
        this.companyId = companyId;
        this.projectId = projectId;
    }

    public static PermissionEntity fromDomain(Permission permission) {
        if (permission == null) {
            return null;
        }
        SecurityEntity securityEntity = null;
        if (permission.getSecurity() != null) {
            if (permission.getSecurity() instanceof User) {
                securityEntity = UserEntity.fromDomain((User) permission.getSecurity());
            } else if (permission.getSecurity() instanceof Group){
                securityEntity = GroupEntity.fromDomain((Group) permission.getSecurity());
            } else if (permission.getSecurity() instanceof Profile) {
                securityEntity = ProfileEntity.fromDomain((Profile) permission.getSecurity());
            }
        }

        return PermissionEntity.builder()
                .id(permission.getId().toString())
                .code(permission.getCode())
                .version(permission.getVersion())
                .createEntityDate(permission.getCreateEntityDate())
                .updateEntityDate(permission.getUpdateEntityDate())
                .createdByUser(permission.getCreatedByUser())
                .lastModifiedByUser(permission.getLastModifiedByUser())
                .security(securityEntity)
                .action(ActionEntity.fromDomain(permission.getAction()))
                .build();
    }

    public Permission toDomain() {
        Security security = null;
        if (this.security != null) {
            if (this.security instanceof UserEntity) {
                security = ((UserEntity) this.security).toDomain();
            } else if (this.security instanceof GroupEntity){
                security = ((GroupEntity) this.security).toDomain();
            } else if (this.security instanceof ProfileEntity) {
                security = ((ProfileEntity) this.security).toDomain();
            }
        }

        return Permission.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .security(security)
                .action(this.action.toDomain())
                .build();
    }

    public PermissionDto toDto() {
        SecurityDto security = null;
        if (this.security != null) {
            if (this.security instanceof UserEntity) {
                security = ((UserEntity) this.security).toDto();
            } else if (this.security instanceof GroupEntity){
                security = ((GroupEntity) this.security).toDto();
            } else if (this.security instanceof ProfileEntity) {
                security = ((ProfileEntity) this.security).toDto();
            }
        }

        return PermissionDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .security(security)
                .action(this.action.toDto())
                .build();
    }

    @JsonIgnore
    @Transient
    public boolean allowAllTenantsAndCompaniesAndProjects() {
        return tenantId==null && companyId == null && projectId==null;
    }
}
