package br.com.archbase.security.service;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.AccessSubject;
import br.com.archbase.security.access.ArchbaseAccessEvaluator;
import br.com.archbase.security.access.DefaultArchbaseAccessEvaluator;
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

    /**
     * O core de decisão. Opcional na injeção para que o serviço continue construível fora do
     * contêiner — ver {@link #evaluator()}.
     */
    @Autowired(required = false)
    private ArchbaseAccessEvaluator accessEvaluator;

    public boolean hasPermission(Authentication authentication, String action, String resource, String tenantId, String companyId, String projectId) {
        return decide(authentication, action, resource, tenantId, companyId, projectId).allowed();
    }

    /**
     * A mesma decisão de {@link #hasPermission}, com o motivo junto.
     *
     * <p>A informação de qual grupo ou perfil concedeu o acesso sempre veio da consulta e era
     * descartada pelo {@code anyMatch}. É ela que permite explicar um acesso sem abrir grupo por
     * grupo no admin, e é a base da tela de efetivo do usuário e da simulação.
     */
    public AccessDecision decide(Authentication authentication, String action, String resource,
                                 String tenantId, String companyId, String projectId) {
        AccessSubject subject = subjectOf(authentication);
        AccessRequirement requirement =
                AccessRequirement.of(resource, action, tenantId, companyId, projectId);
        return evaluator().decide(subject, requirement);
    }

    /**
     * Resolve o sujeito do {@link Authentication}.
     *
     * <p>Devolve {@code null} quando o principal não é um {@code UserEntity} — em vez do
     * {@code ClassCastException} de antes, que virava negação com stack trace apontando para o
     * lugar errado. A decisão continua sendo negar; o que muda é que agora ela diz por quê.
     */
    private AccessSubject subjectOf(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity userEntity)) {
            return null;
        }
        return AccessSubject.of(userEntity);
    }

    /**
     * O avaliador injetado pelo Spring; fora do contêiner, um padrão construído sobre o repositório.
     *
     * <p>A construção tardia existe para que o serviço continue utilizável com
     * {@code new ArchbaseSecurityService()} — como fazem os testes unitários que já cobriam este
     * comportamento antes do core.
     */
    private ArchbaseAccessEvaluator evaluator() {
        ArchbaseAccessEvaluator atual = this.accessEvaluator;
        if (atual == null) {
            atual = new DefaultArchbaseAccessEvaluator(permissionRepository);
            this.accessEvaluator = atual;
        }
        return atual;
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
     * Coleta todos os IDs de segurança relacionados ao usuário (usuário, grupos e perfil).
     */
    private Set<String> collectSecurityIds(UserEntity user) {
        Set<String> ids = new HashSet<>();

        if (user.getId() != null) {
            ids.add(user.getId());
        }

        if (user.getGroups() != null) {
            user.getGroups().stream()
                    .filter(ug -> ug.getGroup() != null && ug.getGroup().getId() != null)
                    .map(ug -> ug.getGroup().getId())
                    .forEach(ids::add);
        }

        if (user.getProfile() != null && user.getProfile().getId() != null) {
            ids.add(user.getProfile().getId());
        }

        return ids;
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
