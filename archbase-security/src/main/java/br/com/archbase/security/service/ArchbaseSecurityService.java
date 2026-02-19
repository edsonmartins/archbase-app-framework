package br.com.archbase.security.service;

import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

import java.util.*;

@Configuration
public class ArchbaseSecurityService {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseSecurityService.class);

    @Autowired
    private PermissionJpaRepository permissionRepository;

    public boolean hasPermission(Authentication authentication, String action, String resource, String tenantId, String companyId, String projectId) {
        if (((UserEntity) authentication.getPrincipal()).getIsAdministrator() &&  ((UserEntity) authentication.getPrincipal()).isEnabled()){
            return  true;
        }
        String userId = ((UserEntity) authentication.getPrincipal()).getId();
        List<PermissionEntity> permissions = permissionRepository.findBySecurityIdAndActionNameAndResourceName(
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

    /**
     * Busca todas as permissões de um usuário, consolidando:
     * - Permissões diretas do usuário
     * - Permissões dos grupos do usuário
     * - Permissões do perfil do usuário
     *
     * @param user Usuário autenticado
     * @return Lista de permissões agrupadas por recurso
     */
    public List<ResourcePermissionsDto> getPermissionsForUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }

        try {
            // Se é administrador, retorna flag especial indicando acesso total
            if (Boolean.TRUE.equals(user.getIsAdministrator())) {
                log.debug("Usuário {} é administrador, retornando permissões totais", user.getEmail());
                return List.of(ResourcePermissionsDto.builder()
                        .resourceName("*")
                        .permissions(Set.of("READ", "CREATE", "UPDATE", "DELETE"))
                        .build());
            }

            // Coleta IDs de segurança (user, groups, profile)
            Set<String> securityIds = collectSecurityIds(user);

            if (securityIds.isEmpty()) {
                log.debug("Nenhum ID de segurança encontrado para usuário: {}", user.getEmail());
                return Collections.emptyList();
            }

            // Busca todas as permissões desses IDs
            List<PermissionEntity> permissions = permissionRepository.findAllBySecurityIds(securityIds);

            // Agrupa por recurso
            return groupPermissionsByResource(permissions);

        } catch (Exception e) {
            log.error("Erro ao buscar permissões para usuário {}: {}", user.getEmail(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Coleta todos os IDs de segurança relacionados ao usuário.
     */
    private Set<String> collectSecurityIds(User user) {
        Set<String> ids = new HashSet<>();

        // ID do próprio usuário
        if (user.getId() != null) {
            ids.add(user.getId().toString());
        }

        // IDs dos grupos
        if (user.getGroups() != null) {
            user.getGroups().stream()
                    .filter(ug -> ug.getGroup() != null && ug.getGroup().getId() != null)
                    .map(ug -> ug.getGroup().getId().toString())
                    .forEach(ids::add);
        }

        // ID do perfil
        if (user.getProfile() != null && user.getProfile().getId() != null) {
            ids.add(user.getProfile().getId().toString());
        }

        log.debug("IDs de segurança coletados para usuário {}: {}", user.getEmail(), ids);
        return ids;
    }

    /**
     * Agrupa permissões por recurso, consolidando as ações.
     */
    private List<ResourcePermissionsDto> groupPermissionsByResource(List<PermissionEntity> permissions) {
        Map<String, Set<String>> resourceActions = new LinkedHashMap<>();

        for (PermissionEntity permission : permissions) {
            if (permission.getAction() != null && permission.getAction().getResource() != null) {
                String resourceName = permission.getAction().getResource().getName();
                String actionName = permission.getAction().getName();

                resourceActions
                        .computeIfAbsent(resourceName, k -> new TreeSet<>())
                        .add(actionName);
            }
        }

        return resourceActions.entrySet().stream()
                .map(entry -> ResourcePermissionsDto.builder()
                        .resourceName(entry.getKey())
                        .permissions(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(ResourcePermissionsDto::getResourceName))
                .toList();
    }
}
