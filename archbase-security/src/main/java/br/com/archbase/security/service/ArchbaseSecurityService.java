package br.com.archbase.security.service;

import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

import java.util.List;

@Configuration
public class ArchbaseSecurityService {
    @Autowired
    private PermissionJpaRepository permissionRepository;

    public boolean hasPermission(Authentication authentication, String action, String resource, String tenantId, String companyId, String projectId) {
        String userId = ((User) authentication.getPrincipal()).getId().toString();
        List<PermissionEntity> permissions = permissionRepository.findByUserIdAndActionNameAndResourceName(
                userId, action, resource);

        if (permissions.stream().anyMatch(PermissionEntity::allowAllTenantsAndCompaniesAndProjects)){
            return true;
        }

        // Verifica permissão considerando tenantId, empresaId e projetoId se fornecidos
        return permissions.stream().anyMatch(permission ->
                (tenantId == null || permission.getTenantId() == null || tenantId.equals(permission.getTenantId())) &&
                        (companyId == null || permission.getCompanyId() == null || companyId.equals(permission.getCompanyId())) &&
                        (projectId == null || permission.getProjectId() == null || projectId.equals(permission.getProjectId()))
        );
    }
}
