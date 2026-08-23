package br.com.archbase.security.service;

import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.SecurityEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.UserGroupEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre {@link ArchbaseSecurityService#hasPermission}: permissão concedida diretamente ao
 * usuário, ao perfil do usuário ou a um grupo do usuário devem liberar o acesso da mesma forma.
 */
class ArchbaseSecurityServiceTest {

    private static final String ACTION = "VIEW";
    private static final String RESOURCE = "PRODUCT";

    private PermissionJpaRepository permissionRepository;
    private ArchbaseSecurityService securityService;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionJpaRepository.class);
        securityService = new ArchbaseSecurityService();
        ReflectionTestUtils.setField(securityService, "permissionRepository", permissionRepository);
    }

    @Test
    void permissaoDiretaDoUsuarioLiberaAcesso() {
        UserEntity user = userComId("user-1");
        PermissionEntity permission = permissionPara(securityComId("user-1"), null, null, null);
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(List.of(permission));

        assertTrue(securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, null, null, null));
    }

    @Test
    void permissaoConcedidaAoPerfilDoUsuarioLiberaAcesso() {
        ProfileEntity profile = profileComId("profile-1");
        UserEntity user = userComId("user-1");
        user.setProfile(profile);

        PermissionEntity permission = permissionPara(profile, null, null, null);
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(List.of(permission));

        assertTrue(securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, null, null, null));
    }

    @Test
    void permissaoConcedidaAUmGrupoDoUsuarioLiberaAcesso() {
        GroupEntity group = groupComId("group-1");
        UserEntity user = userComId("user-1");
        user.setGroups(Set.of(userGroupPara(user, group)));

        PermissionEntity permission = permissionPara(group, null, null, null);
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(List.of(permission));

        assertTrue(securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, null, null, null));
    }

    @Test
    void usuarioSemPermissaoDiretaNemViaPerfilOuGrupoNaoTemAcesso() {
        UserEntity user = userComId("user-1");
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(Collections.emptyList());

        assertFalse(securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, null, null, null));
    }

    @Test
    void administradorHabilitadoTemAcessoSemPrecisarDeConcessao() {
        UserEntity admin = userComId("admin-1");
        admin.setIsAdministrator(true);
        admin.setAccountDeactivated(false);
        admin.setAccountLocked(false);

        // A consulta acontece — o administrador precisa ser alcançável por uma NEGAÇÃO explícita,
        // e sem consultar ela seria gravada e ignorada. O que se afirma aqui é que nenhuma
        // CONCESSÃO precisa existir: a flag basta.
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(Collections.emptyList());

        assertTrue(securityService.hasPermission(authenticationPara(admin), ACTION, RESOURCE, null, null, null));
    }

    @Test
    void conjuntoDeIdsConsultadoIncluiUsuarioGrupoEPerfil() {
        ProfileEntity profile = profileComId("profile-1");
        GroupEntity group = groupComId("group-1");
        UserEntity user = userComId("user-1");
        user.setProfile(profile);
        user.setGroups(Set.of(userGroupPara(user, group)));

        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(Collections.emptyList());

        securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, null, null, null);

        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(permissionRepository).findBySecurityIdsAndActionNameAndResourceName(captor.capture(), eq(ACTION), eq(RESOURCE), anyBoolean());
        assertEquals(Set.of("user-1", "group-1", "profile-1"), captor.getValue());
    }

    @Test
    void permissaoComAllowAllLiberaAcessoIndependenteDoTenant() {
        UserEntity user = userComId("user-1");
        PermissionEntity permission = permissionPara(securityComId("user-1"), null, null, null);
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(List.of(permission));

        assertTrue(securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, "tenant-x", "company-x", "project-x"));
    }

    @Test
    void permissaoComTenantEspecificoSoLiberaParaOTenantCorrespondente() {
        UserEntity user = userComId("user-1");
        PermissionEntity permission = permissionPara(securityComId("user-1"), "tenant-a", null, null);
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), eq(ACTION), eq(RESOURCE), anyBoolean()))
                .thenReturn(List.of(permission));

        assertFalse(securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, "tenant-b", null, null));
        assertTrue(securityService.hasPermission(authenticationPara(user), ACTION, RESOURCE, "tenant-a", null, null));
    }

    private static UserEntity userComId(String id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("Usuário " + id);
        user.setDescription("Usuário de teste");
        user.setIsAdministrator(false);
        user.setAccountDeactivated(false);
        user.setAccountLocked(false);
        return user;
    }

    private static ProfileEntity profileComId(String id) {
        return ProfileEntity.builder().id(id).name("Perfil " + id).description("Perfil de teste").build();
    }

    private static GroupEntity groupComId(String id) {
        return GroupEntity.builder().id(id).name("Grupo " + id).description("Grupo de teste").build();
    }

    private static UserEntity securityComId(String id) {
        UserEntity security = new UserEntity();
        security.setId(id);
        security.setName("Segurança " + id);
        security.setDescription("Segurança de teste");
        return security;
    }

    private static UserGroupEntity userGroupPara(UserEntity user, GroupEntity group) {
        return UserGroupEntity.builder().id("ug-" + group.getId()).user(user).group(group).build();
    }

    private static PermissionEntity permissionPara(SecurityEntity security,
                                                     String tenantId, String companyId, String projectId) {
        ResourceEntity resourceEntity = ResourceEntity.builder().id("resource-1").name(RESOURCE).description("Recurso de teste").build();
        ActionEntity actionEntity = ActionEntity.builder().id("action-1").name(ACTION).description("Ação de teste").resource(resourceEntity).build();
        return PermissionEntity.builder()
                .id("permission-1")
                .security(security)
                .action(actionEntity)
                .tenantId(tenantId)
                .companyId(companyId)
                .projectId(projectId)
                .build();
    }

    private static Authentication authenticationPara(UserEntity user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }
}
