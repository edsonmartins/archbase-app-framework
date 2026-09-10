package br.com.archbase.security.integration;

import br.com.archbase.security.access.EffectiveCapability;
import br.com.archbase.security.diagnostics.ArchbaseAccessDiagnosticsService;
import br.com.archbase.security.diagnostics.EffectiveAccessReport;
import br.com.archbase.security.domain.dto.CapabilityDependencyNodeDto;
import br.com.archbase.security.domain.dto.CapabilityDependencyTreeDto;
import br.com.archbase.security.domain.dto.PermissionWithTypesDto;
import br.com.archbase.security.domain.dto.ResoucePermissionsWithTypeDto;
import br.com.archbase.security.domain.dto.ResourceRegisterDto;
import br.com.archbase.security.domain.dto.SimpleActionDto;
import br.com.archbase.security.domain.dto.SimpleResourceDto;
import br.com.archbase.security.domain.entity.DependencySource;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ActionDependencyEntity;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.SecurityEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.repository.ActionDependencyJpaRepository;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.GroupJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseCapabilityDependencyService;
import br.com.archbase.security.service.ResourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As arestas de dependência entre capacidades — passos 1 a 3 do
 * {@code CONTRATO_DEPENDENCIAS_DE_CAPACIDADE.md}.
 *
 * <p>O que precisa de rede aqui não é "cria a linha", é a <b>poda</b>. Reconciliação errada neste
 * módulo já custou caro duas vezes: a varredura desativou 8 recursos criados por seed no gestor-rq,
 * e o registro de tela com payload vazio zerou as ações de 56 dos 100 recursos. As duas regras deste
 * passo — integral para {@code SCAN}, escopada à ação para {@code REGISTER} — existem por causa
 * disso, e é isso que estes testes fixam.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-dependencias-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste",
        "archbase.security.diagnostics.enabled=true"
})
@DisplayName("Dependências entre capacidades (Spring + H2)")
class DependenciasDeCapacidadeIntegrationTest {

    private static final String OS = "tms.ordemservico";
    private static final String CLIENTE = "tms.cliente";
    private static final String COCKPIT = "Cockpit";

    @Autowired
    ArchbaseCapabilityDependencyService dependencyService;
    @Autowired
    ResourceService resourceService;
    @Autowired
    ActionDependencyJpaRepository dependencyRepository;
    @Autowired
    ActionJpaRepository actionRepository;
    @Autowired
    ResourceJpaRepository resourceRepository;
    @Autowired
    PermissionJpaRepository permissionRepository;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    GroupJpaRepository groupRepository;
    @Autowired
    ResourcePersistenceAdapter adapter;
    @Autowired
    ArchbaseAccessDiagnosticsService diagnostics;

    private final AtomicInteger sequencia = new AtomicInteger();
    private ResourceEntity os;
    private ResourceEntity cliente;

    @BeforeEach
    void limpar() {
        dependencyRepository.deleteAll();
        permissionRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();

        os = recurso("res-os", OS, TipoRecurso.API);
        cliente = recurso("res-cliente", CLIENTE, TipoRecurso.API);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("varredura do código (SCAN)")
    class Varredura {

        @Test
        @DisplayName("grava a aresta e resolve o alvo que já está no catálogo")
        void gravaEResolve() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            ActionEntity ver = acao(os, "view");

            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            List<ActionDependencyEntity> arestas = dependencyRepository.findAll();
            assertThat(arestas).hasSize(1);
            assertThat(arestas.get(0).getRequiredCapability()).isEqualTo(OS + ":view");
            assertThat(arestas.get(0).getRequiredAction().getId()).isEqualTo(ver.getId());
            assertThat(arestas.get(0).getDeclaredBy()).isEqualTo(DependencySource.SCAN);
            assertThat(arestas.get(0).isUnresolved()).isFalse();
        }

        @Test
        @DisplayName("alvo inexistente vira aresta PENDENTE, não some")
        void alvoInexistentePermanece() {
            // Erro de digitação, módulo não implantado, recurso que nenhum admin abriu ainda. A
            // aresta precisa ficar visível — sumir em silêncio é o defeito que zerou 56 recursos.
            ActionEntity aprovar = acao(os, "aprovar_custo");

            dependencyService.reconcileScan(Map.of(aprovar, Set.of("modulo.ausente:view")), false);

            assertThat(dependencyRepository.findUnresolved()).hasSize(1);
            assertThat(dependencyService.countUnresolved()).isEqualTo(1);
        }

        @Test
        @DisplayName("a pendente passa a valer sozinha quando o alvo entra no catálogo")
        void pendenteResolveDepois() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(CLIENTE + ":view")), false);
            assertThat(dependencyService.countUnresolved()).isEqualTo(1);

            ActionEntity alvo = acao(cliente, "view");

            assertThat(dependencyService.resolvePending()).isEqualTo(1);
            assertThat(dependencyService.countUnresolved()).isZero();
            assertThat(dependencyRepository.findAll().get(0).getRequiredAction().getId())
                    .isEqualTo(alvo.getId());
        }

        @Test
        @DisplayName("aresta que o código deixou de declarar é removida")
        void removeODeixadoDeDeclarar() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            acao(os, "view");
            acao(cliente, "view");

            dependencyService.reconcileScan(
                    Map.of(aprovar, Set.of(OS + ":view", CLIENTE + ":view")), false);
            assertThat(dependencyRepository.findAll()).hasSize(2);

            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            assertThat(dependencyRepository.findAll())
                    .extracting(ActionDependencyEntity::getRequiredCapability)
                    .containsExactly(OS + ":view");
        }

        @Test
        @DisplayName("capacidade que sumiu do código perde todas as arestas")
        void capacidadeQueSumiu() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            ActionEntity cancelar = acao(os, "cancelar");
            acao(os, "view");

            dependencyService.reconcileScan(Map.of(
                    aprovar, Set.of(OS + ":view"),
                    cancelar, Set.of(OS + ":view")), false);
            assertThat(dependencyRepository.findAll()).hasSize(2);

            // 'cancelar' deixou de existir no código — a reconciliação é integral.
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            assertThat(dependencyRepository.findAll()).hasSize(1);
            assertThat(dependencyRepository.findByActionIdAndDeclaredBy(
                    cancelar.getId(), DependencySource.SCAN)).isEmpty();
        }

        @Test
        @DisplayName("declarar de novo o mesmo conjunto não cria linha duplicada")
        void idempotente() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            acao(os, "view");

            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);
            String idAntes = dependencyRepository.findAll().get(0).getId();

            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            assertThat(dependencyRepository.findAll()).hasSize(1);
            assertThat(dependencyRepository.findAll().get(0).getId()).isEqualTo(idAntes);
        }

        @Test
        @DisplayName("modo relatório não escreve nada")
        void relatorioNaoEscreve() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            acao(os, "view");

            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), true);

            assertThat(dependencyRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("modo relatório também não REMOVE — é o passo que se roda antes de aplicar")
        void relatorioNaoRemove() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            acao(os, "view");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            dependencyService.reconcileScan(Map.of(), true);

            assertThat(dependencyRepository.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("registro de tela (REGISTER)")
    class RegistroDeTela {

        @Test
        @DisplayName("a poda é escopada à ação — outra ação do mesmo recurso não é tocada")
        void podaEscopadaAAcao() {
            // A razão de o registro ser aditivo: um recurso pode ser declarado por mais de uma tela,
            // e cada uma envia só as ações que usa.
            ActionEntity abrir = acao(os, "abrir");
            ActionEntity fechar = acao(os, "fechar");
            acao(cliente, "view");

            dependencyService.reconcileRegister(abrir, Set.of(CLIENTE + ":view"));
            dependencyService.reconcileRegister(fechar, Set.of(CLIENTE + ":view"));
            assertThat(dependencyRepository.findAll()).hasSize(2);

            // Uma segunda tela registra apenas 'abrir', agora sem dependência nenhuma.
            dependencyService.reconcileRegister(abrir, Set.of());

            assertThat(dependencyRepository.findByActionIdAndDeclaredBy(
                    abrir.getId(), DependencySource.REGISTER)).isEmpty();
            assertThat(dependencyRepository.findByActionIdAndDeclaredBy(
                    fechar.getId(), DependencySource.REGISTER)).hasSize(1);
        }

        @Test
        @DisplayName("não toca nas arestas que a varredura declarou para a MESMA ação")
        void naoTocaNasDoScan() {
            ActionEntity abrir = acao(os, "abrir");
            acao(os, "view");
            acao(cliente, "view");

            dependencyService.reconcileScan(Map.of(abrir, Set.of(OS + ":view")), false);
            dependencyService.reconcileRegister(abrir, Set.of(CLIENTE + ":view"));

            assertThat(dependencyRepository.findByActionIdAndDeclaredBy(
                    abrir.getId(), DependencySource.SCAN)).hasSize(1);
            assertThat(dependencyRepository.findByActionIdAndDeclaredBy(
                    abrir.getId(), DependencySource.REGISTER)).hasSize(1);
        }

        @Test
        @DisplayName("nulo é 'não declarei' e não remove nada")
        void nuloNaoRemove() {
            // É o que todo cliente anterior envia. Tratar como lista vazia apagaria as arestas de
            // todo mundo que ainda não atualizou o frontend.
            ActionEntity abrir = acao(os, "abrir");
            acao(cliente, "view");
            dependencyService.reconcileRegister(abrir, Set.of(CLIENTE + ":view"));

            dependencyService.reconcileRegister(abrir, null);

            assertThat(dependencyRepository.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("ponta a ponta pelo registerResource")
    class PontaAPonta {

        @Test
        @DisplayName("a tela declara o endpoint que seu botão aciona, e a aresta é gravada resolvida")
        void telaDeclaraEndpoint() {
            ActionEntity alvo = acao(os, "aprovar_custo");
            autenticarAdministrador();

            resourceService.registerResource(ResourceRegisterDto.builder()
                    .resource(SimpleResourceDto.builder()
                            .resourceName(COCKPIT).resourceDescription("Cockpit do vendedor").build())
                    .actions(List.of(SimpleActionDto.builder()
                            .actionName("aprovar").actionDescription("Aprovar")
                            .requires(List.of(OS + ":aprovar_custo"))
                            .build()))
                    .build());

            List<ActionDependencyEntity> arestas = dependencyRepository.findAll();
            assertThat(arestas).hasSize(1);
            assertThat(arestas.get(0).getDeclaredBy()).isEqualTo(DependencySource.REGISTER);
            assertThat(arestas.get(0).getRequiredCapability()).isEqualTo(OS + ":aprovar_custo");
            assertThat(arestas.get(0).getRequiredAction().getId()).isEqualTo(alvo.getId());
        }

        @Test
        @DisplayName("payload sem 'requires' — todo cliente anterior — não grava nem apaga nada")
        void payloadAntigoNaoMexe() {
            autenticarAdministrador();

            resourceService.registerResource(ResourceRegisterDto.builder()
                    .resource(SimpleResourceDto.builder()
                            .resourceName(COCKPIT).resourceDescription("Cockpit do vendedor").build())
                    .actions(List.of(SimpleActionDto.builder()
                            .actionName("aprovar").actionDescription("Aprovar").build()))
                    .build());

            assertThat(dependencyRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("dependência inválida é descartada, e o resto do registro segue valendo")
        void invalidaNaoDerrubaORegistro() {
            autenticarAdministrador();

            resourceService.registerResource(ResourceRegisterDto.builder()
                    .resource(SimpleResourceDto.builder()
                            .resourceName(COCKPIT).resourceDescription("Cockpit do vendedor").build())
                    .actions(List.of(SimpleActionDto.builder()
                            .actionName("aprovar").actionDescription("Aprovar")
                            .requires(List.of("a:b:c"))
                            .build()))
                    .build());

            assertThat(dependencyRepository.findAll()).isEmpty();
            // A capacidade continua catalogada: o defeito estava na aresta, não no registro.
            assertThat(actionRepository.findByActionNameAndResourceName("aprovar", COCKPIT))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("leitura pelo catálogo (passo 4)")
    class LeituraPeloCatalogo {

        @Test
        @DisplayName("o catálogo traz as dependências DIRETAS de cada capacidade")
        void catalogoTrazAsDiretas() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            acao(os, "view");
            acao(cliente, "view");
            dependencyService.reconcileScan(
                    Map.of(aprovar, Set.of(OS + ":view", CLIENTE + ":view")), false);

            PermissionWithTypesDto capacidade = doCatalogo(adapter.findAllResourcesPermissions(),
                    OS, "aprovar_custo");

            assertThat(capacidade.getRequires())
                    .containsExactly(CLIENTE + ":view", OS + ":view");
        }

        @Test
        @DisplayName("capacidade sem dependência não ganha o campo — a resposta de antes, intacta")
        void semDependenciaOmiteOCampo() {
            acao(os, "view");

            assertThat(doCatalogo(adapter.findAllResourcesPermissions(), OS, "view").getRequires())
                    .isNull();
        }

        @Test
        @DisplayName("a listagem do usuário também traz — é dela que sai o aviso ao revogar")
        void listagemDoUsuarioTambemTraz() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            acao(os, "view");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            UserEntity user = usuario("user-1");
            conceder(user, aprovar);

            assertThat(doCatalogo(adapter.findUserResourcesPermissions(user.getId()),
                    OS, "aprovar_custo").getRequires())
                    .containsExactly(OS + ":view");
        }
    }

    @Nested
    @DisplayName("fecho transitivo (passo 4)")
    class FechoTransitivo {

        @Test
        @DisplayName("alcança o que depende do que depende, com a profundidade e o caminho")
        void alcancaIndiretas() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            ActionEntity ver = acao(os, "view");
            ActionEntity verCliente = acao(cliente, "view");

            dependencyService.reconcileScan(Map.of(
                    aprovar, Set.of(OS + ":view"),
                    ver, Set.of(CLIENTE + ":view")), false);

            CapabilityDependencyTreeDto fecho = dependencyService.closureOf(aprovar.getId()).orElseThrow();

            assertThat(fecho.getCapability()).isEqualTo(OS + ":aprovar_custo");
            assertThat(fecho.isTruncated()).isFalse();
            assertThat(fecho.getDependencies())
                    .extracting(CapabilityDependencyNodeDto::getCapability)
                    .containsExactly(OS + ":view", CLIENTE + ":view");

            CapabilityDependencyNodeDto indireta = fecho.getDependencies().get(1);
            assertThat(indireta.getDepth()).isEqualTo(2);
            assertThat(indireta.getRequiredBy()).isEqualTo(OS + ":view");
            assertThat(indireta.getActionId()).isEqualTo(verCliente.getId());
            assertThat(indireta.isResolved()).isTrue();
        }

        @Test
        @DisplayName("ciclo não trava o percurso")
        void cicloNaoTrava() {
            // A → B e B → A é uma declaração possível. O conjunto de visitados encerra o ramo.
            ActionEntity a = acao(os, "a");
            ActionEntity b = acao(os, "b");
            dependencyService.reconcileScan(Map.of(
                    a, Set.of(OS + ":b"),
                    b, Set.of(OS + ":a")), false);

            CapabilityDependencyTreeDto fecho = dependencyService.closureOf(a.getId()).orElseThrow();

            assertThat(fecho.getDependencies())
                    .extracting(CapabilityDependencyNodeDto::getCapability)
                    .containsExactly(OS + ":b");
            assertThat(fecho.isTruncated()).isFalse();
        }

        @Test
        @DisplayName("dependência não resolvida entra marcada, e não é expandida")
        void naoResolvidaEntraMarcada() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of("modulo.ausente:view")), false);

            CapabilityDependencyNodeDto no =
                    dependencyService.closureOf(aprovar.getId()).orElseThrow().getDependencies().get(0);

            assertThat(no.getCapability()).isEqualTo("modulo.ausente:view");
            assertThat(no.isResolved()).isFalse();
            assertThat(no.getActionId()).isNull();
            assertThat(no.getResourceName()).isEqualTo("modulo.ausente");
        }

        @Test
        @DisplayName("cadeia mais longa que o teto vem marcada como truncada")
        void cadeiaLongaTrunca() {
            // Onze elos: o teto é 10, e a resposta precisa DIZER que parou.
            ActionEntity anterior = acao(os, "elo-0");
            ActionEntity primeira = anterior;
            java.util.Map<ActionEntity, Set<String>> declaradas = new java.util.LinkedHashMap<>();
            for (int i = 1; i <= 11; i++) {
                ActionEntity atual = acao(os, "elo-" + i);
                declaradas.put(anterior, Set.of(OS + ":elo-" + i));
                anterior = atual;
            }
            dependencyService.reconcileScan(declaradas, false);

            CapabilityDependencyTreeDto fecho =
                    dependencyService.closureOf(primeira.getId()).orElseThrow();

            assertThat(fecho.isTruncated()).isTrue();
            assertThat(fecho.getDependencies()).hasSize(10);
        }

        @Test
        @DisplayName("capacidade inexistente devolve vazio, não uma árvore vazia")
        void inexistenteDevolveVazio() {
            assertThat(dependencyService.closureOf("nao-existe")).isEmpty();
        }
    }

    @Nested
    @DisplayName("relatório de efetivo (passo 5)")
    class RelatorioDeEfetivo {

        @Test
        @DisplayName("marca a dependência que a pessoa NÃO alcança")
        void marcaANaoAtendida() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            acao(os, "view");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            UserEntity user = usuario("user-1");
            conceder(user, aprovar);

            EffectiveAccessReport relatorio = diagnostics.effective(user.getId(), null).orElseThrow();
            EffectiveCapability capacidade = doRelatorio(relatorio, OS + ":aprovar_custo");

            assertThat(capacidade.unmetDependencies()).containsExactly(OS + ":view");
            // A situação NÃO muda: é o que a decisão faz com ela — deixa passar.
            assertThat(capacidade.situation()).isEqualTo(EffectiveCapability.Situation.EFFECTIVE);
        }

        @Test
        @DisplayName("dependência que a pessoa alcança não é marcada")
        void atendidaNaoEMarcada() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            ActionEntity ver = acao(os, "view");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            UserEntity user = usuario("user-1");
            conceder(user, aprovar);
            conceder(user, ver);

            EffectiveAccessReport relatorio = diagnostics.effective(user.getId(), null).orElseThrow();

            assertThat(doRelatorio(relatorio, OS + ":aprovar_custo").unmetDependencies()).isEmpty();
        }

        @Test
        @DisplayName("alcançar pelo GRUPO conta — a dependência não é marcada")
        void alcancarPeloGrupoConta() {
            ActionEntity aprovar = acao(os, "aprovar_custo");
            ActionEntity ver = acao(os, "view");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            GroupEntity grupo = grupo("GESTORES");
            UserEntity user = vincular(usuario("user-1"), grupo);
            conceder(user, aprovar);
            conceder(grupo, ver);

            EffectiveAccessReport relatorio = diagnostics.effective(user.getId(), null).orElseThrow();

            assertThat(doRelatorio(relatorio, OS + ":aprovar_custo").unmetDependencies()).isEmpty();
        }

        @Test
        @DisplayName("concessão sobre ação INATIVA não satisfaz a dependência")
        void inerteNaoSatisfaz() {
            // Ela não leva a pessoa a lugar nenhum: a tela já a ignora, e o require-active a
            // desligará também no backend. Contá-la esconderia o problema que o relatório existe
            // para mostrar.
            ActionEntity aprovar = acao(os, "aprovar_custo");
            ActionEntity ver = acaoInativa(os, "view");
            dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);

            UserEntity user = usuario("user-1");
            conceder(user, aprovar);
            conceder(user, ver);

            EffectiveAccessReport relatorio = diagnostics.effective(user.getId(), null).orElseThrow();

            assertThat(doRelatorio(relatorio, OS + ":aprovar_custo").unmetDependencies())
                    .containsExactly(OS + ":view");
        }
    }

    // ---- fixtura ----

    private ResourceEntity recurso(String id, String nome, TipoRecurso tipo) {
        return resourceRepository.save(ResourceEntity.builder()
                .id(id).name(nome).description("Recurso " + nome).active(true).type(tipo)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private ActionEntity acao(ResourceEntity recurso, String nome) {
        return salvarAcao(recurso, nome, true);
    }

    private ActionEntity acaoInativa(ResourceEntity recurso, String nome) {
        return salvarAcao(recurso, nome, false);
    }

    private ActionEntity salvarAcao(ResourceEntity recurso, String nome, boolean ativa) {
        return actionRepository.save(ActionEntity.builder()
                .id("act-" + sequencia.incrementAndGet()).name(nome).description("Ação " + nome)
                .resource(recurso).active(ativa)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private UserEntity usuario(String id) {
        return userRepository.save(UserEntity.builder()
                .id(id).name("Usuário " + id).description("Usuário de teste")
                .userName(id).email(id + "@exemplo.test").password("irrelevante")
                .isAdministrator(false).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true)
                .allowPasswordChange(true).allowMultipleLogins(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private GroupEntity grupo(String nome) {
        return groupRepository.save(GroupEntity.builder()
                .id("group-" + nome).name(nome).description("Grupo " + nome)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private UserEntity vincular(UserEntity user, GroupEntity grupo) {
        java.util.Set<br.com.archbase.security.persistence.UserGroupEntity> vinculos =
                new java.util.HashSet<>();
        vinculos.add(br.com.archbase.security.persistence.UserGroupEntity.builder()
                .id("ug-" + sequencia.incrementAndGet()).user(user).group(grupo).build());
        user.setGroups(vinculos);
        return userRepository.save(user);
    }

    private PermissionEntity conceder(SecurityEntity destinatario, ActionEntity acao) {
        return permissionRepository.save(PermissionEntity.builder()
                .id("perm-" + sequencia.incrementAndGet()).security(destinatario).action(acao)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private PermissionWithTypesDto doCatalogo(java.util.List<ResoucePermissionsWithTypeDto> catalogo,
                                              String recurso, String acao) {
        return catalogo.stream()
                .filter(r -> recurso.equals(r.getResourceName()))
                .flatMap(r -> r.getPermissions().stream())
                .filter(c -> acao.equals(c.getActionName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Capacidade ausente: " + recurso + ":" + acao));
    }

    private EffectiveCapability doRelatorio(EffectiveAccessReport relatorio, String capacidade) {
        return relatorio.capabilities().stream()
                .filter(c -> capacidade.equals(c.capability()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Capacidade ausente no relatório: " + capacidade));
    }

    /** O registro de tela é endpoint administrativo, e o serviço lê o usuário logado. */
    private void autenticarAdministrador() {
        UserEntity admin = userRepository.save(UserEntity.builder()
                .id("admin-1").name("Admin").description("Administrador de teste")
                .userName("admin").email("admin@exemplo.test").password("irrelevante")
                .isAdministrator(true).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true)
                .allowPasswordChange(true).allowMultipleLogins(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }
}
