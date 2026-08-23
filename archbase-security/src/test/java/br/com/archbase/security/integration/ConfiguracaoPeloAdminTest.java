package br.com.archbase.security.integration;

import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.access.PermissionEffect;
import br.com.archbase.security.adapter.ActionPersistenceAdapter;
import br.com.archbase.security.adapter.UserProfilePersistenceAdapter;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ProfileJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As três colunas do core podem ser configuradas <b>pelo admin</b>, e não só por SQL.
 *
 * <p><b>Por que este teste existe.</b> Uma revisão encontrou que {@code MINIMUM_LEVEL},
 * {@code ACCESS_LEVEL} e {@code EFFECT} existiam nas entidades e no avaliador, mas não em nenhum
 * DTO nem em nenhum caminho de escrita: as features estavam implementadas e <b>inalcançáveis</b>
 * pela interface. A documentação afirmava o contrário — "a partir daí quem manda é o admin" — e a
 * mensagem do validador de subida mandava "preencha o mínimo no admin", onde não havia campo algum.
 *
 * <p>Cada cenário abaixo escreve pelo mesmo caminho que a tela usa e lê de volta.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-config-admin-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste"
})
@DisplayName("Configuração do core pelo admin (Spring + H2)")
class ConfiguracaoPeloAdminTest {

    @Autowired
    ActionPersistenceAdapter actionAdapter;
    @Autowired
    UserProfilePersistenceAdapter profileAdapter;
    @Autowired
    ActionJpaRepository actionRepository;
    @Autowired
    ResourceJpaRepository resourceRepository;
    @Autowired
    ProfileJpaRepository profileRepository;
    @Autowired
    PermissionJpaRepository permissionRepository;

    private ResourceEntity recurso;

    @BeforeEach
    void limpar() {
        permissionRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();
        profileRepository.deleteAll();

        recurso = resourceRepository.save(ResourceEntity.builder()
                .id("resource-1").name("tms.ordemservico").description("OS").active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    @Test
    @DisplayName("o nível mínimo da capacidade é editável e volta na leitura")
    void nivelMinimoEditavel() {
        ActionEntity acao = acaoSalva("aprovar_custo", null);

        ActionDto edicao = acao.toDto();
        edicao.setMinimumLevel(AccessLevel.SUPERVISOR);
        Optional<ActionDto> salvo = actionAdapter.updateAction(acao.getId(), edicao);

        assertThat(salvo).isPresent();
        assertThat(salvo.get().getMinimumLevel()).isEqualTo(AccessLevel.SUPERVISOR);
        assertThat(actionRepository.findById(acao.getId()).orElseThrow().getMinimumLevel())
                .isEqualTo(AccessLevel.SUPERVISOR);
    }

    @Test
    @DisplayName("cliente que não conhece o campo NÃO apaga o nível já configurado")
    void clienteAntigoNaoApagaONivel() {
        // A proteção que importa: um admin de versão anterior, ao salvar qualquer edição, enviaria
        // minimumLevel nulo. Apagar o piso de uma capacidade sensível por omissão do cliente seria
        // afrouxar segurança sem que ninguém tivesse decidido nada.
        ActionEntity acao = acaoSalva("aprovar_custo", AccessLevel.SUPERVISOR);

        ActionDto edicaoAntiga = acao.toDto();
        edicaoAntiga.setMinimumLevel(null);
        edicaoAntiga.setDescription("descrição nova");
        actionAdapter.updateAction(acao.getId(), edicaoAntiga);

        ActionEntity depois = actionRepository.findById(acao.getId()).orElseThrow();
        assertThat(depois.getMinimumLevel()).isEqualTo(AccessLevel.SUPERVISOR);
        assertThat(depois.getDescription())
                .as("a edição legítima passou")
                .isEqualTo("descrição nova");
    }

    @Test
    @DisplayName("NONE tira o piso explicitamente")
    void noneTiraOPiso() {
        ActionEntity acao = acaoSalva("view", AccessLevel.SUPERVISOR);

        ActionDto edicao = acao.toDto();
        edicao.setMinimumLevel(AccessLevel.NONE);
        actionAdapter.updateAction(acao.getId(), edicao);

        assertThat(actionRepository.findById(acao.getId()).orElseThrow().getMinimumLevel())
                .as("NONE é a forma de dizer 'sem piso', e vira nulo na coluna")
                .isNull();
    }

    @Test
    @DisplayName("o nível do perfil é editável e volta na leitura")
    void nivelDoPerfilEditavel() {
        ProfileEntity perfil = profileRepository.save(ProfileEntity.builder()
                .id("profile-1").name("SUPERVISOR").description("Supervisão")
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        ProfileDto edicao = perfil.toDto();
        edicao.setAccessLevel(AccessLevel.SUPERVISOR);
        Optional<ProfileDto> salvo = profileAdapter.updateProfile(perfil.getId(), edicao);

        assertThat(salvo).isPresent();
        assertThat(salvo.get().getAccessLevel()).isEqualTo(AccessLevel.SUPERVISOR);
        assertThat(profileRepository.findById(perfil.getId()).orElseThrow().getAccessLevel())
                .isEqualTo(AccessLevel.SUPERVISOR);
    }

    @Test
    @DisplayName("cliente que não conhece o campo NÃO rebaixa o nível do perfil")
    void clienteAntigoNaoRebaixaOPerfil() {
        ProfileEntity perfil = profileRepository.save(ProfileEntity.builder()
                .id("profile-1").name("SUPERVISOR").description("Supervisão")
                .accessLevel(AccessLevel.SUPERVISOR)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        ProfileDto edicaoAntiga = perfil.toDto();
        edicaoAntiga.setAccessLevel(null);
        profileAdapter.updateProfile(perfil.getId(), edicaoAntiga);

        assertThat(profileRepository.findById(perfil.getId()).orElseThrow().getAccessLevel())
                .isEqualTo(AccessLevel.SUPERVISOR);
    }

    @Test
    @DisplayName("o efeito da permissão volta na leitura, com nulo lido como GRANT")
    void efeitoVoltaNaLeitura() {
        ActionEntity acao = acaoSalva("aprovar_custo", null);
        ProfileEntity perfil = profileRepository.save(ProfileEntity.builder()
                .id("profile-1").name("SUPERVISOR").description("Supervisão")
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        PermissionEntity concessao = permissionRepository.save(PermissionEntity.builder()
                .id("perm-1").security(perfil).action(acao)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        assertThat(concessao.toDto().getEffect())
                .as("coluna nula é GRANT, e a leitura precisa dizer isso em vez de devolver nulo")
                .isEqualTo(PermissionEffect.GRANT);

        PermissionEntity negacao = permissionRepository.save(PermissionEntity.builder()
                .id("perm-2").security(perfil).action(acao).effect(PermissionEffect.DENY)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        assertThat(negacao.toDto().getEffect()).isEqualTo(PermissionEffect.DENY);
        assertThat(negacao.isDeny()).isTrue();
    }

    private ActionEntity acaoSalva(String nome, AccessLevel minimo) {
        return actionRepository.save(ActionEntity.builder()
                .id("act-" + nome).name(nome).description("Ação " + nome)
                .resource(recurso).active(true).minimumLevel(minimo)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }
}
