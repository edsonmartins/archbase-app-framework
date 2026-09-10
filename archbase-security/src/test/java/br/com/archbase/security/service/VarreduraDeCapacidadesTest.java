package br.com.archbase.security.service;

import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.annotation.ArchbaseResource;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A varredura que alimenta o catálogo a partir de {@code @HasPermission}.
 *
 * <p>Dois comportamentos que esta fase acrescenta e que precisam de rede:
 *
 * <ul>
 *   <li><b>Modo relatório.</b> Com {@code archbase.security.sync.mode=report} nada é escrito. É o
 *       passo que se roda antes de ligar a primeira anotação num sistema em produção — porque a
 *       varredura, ao não encontrar capacidade alguma, <b>desativa o catálogo de tipo API inteiro</b>,
 *       que foi o que aconteceu no gestor-rq.</li>
 *   <li><b>Herança de recurso.</b> {@code @ArchbaseResource} na classe evita repetir o recurso em
 *       cada método — e só o recurso é herdado, nunca a ação.</li>
 * </ul>
 */
@DisplayName("Varredura de capacidades")
class VarreduraDeCapacidadesTest {

    private ActionJpaRepository actionRepository;
    private ResourceJpaRepository resourceRepository;
    private ArchbaseCapabilityDependencyService dependencyService;
    private ArchbaseActionSynchronizationService service;

    @ArchbaseResource(value = "tms.ordemservico", description = "Ordem de serviço")
    static class ControllerComRecursoNaClasse {

        @HasPermission(action = "view", description = "Listar ordens")
        public void listar() {
        }

        @HasPermission(action = "aprovar_custo", description = "Aprovar custo",
                minimumLevel = AccessLevel.SUPERVISOR)
        public void aprovarCusto() {
        }

        @HasPermission(action = "instalar", description = "Instalar pneu", resource = "tms.pneu")
        public void instalarPneu() {
        }
    }

    static class ControllerSemRecurso {

        @HasPermission(action = "view", description = "Sem recurso declarado")
        public void semRecurso() {
        }
    }

    /** Declara a capacidade, sem saber a que recurso pertence — quem sabe é a subclasse. */
    abstract static class ControllerAbstrato {

        @HasPermission(action = "aprovar", description = "Aprovar")
        public void aprovar() {
        }
    }

    @ArchbaseResource("tms.abastecimento")
    static class ControllerConcreto extends ControllerAbstrato {
    }

    @ArchbaseResource("tms.pneu")
    static class OutroControllerConcreto extends ControllerAbstrato {
    }

    @BeforeEach
    void setUp() {
        actionRepository = mock(ActionJpaRepository.class);
        resourceRepository = mock(ResourceJpaRepository.class);
        dependencyService = mock(ArchbaseCapabilityDependencyService.class);
        service = new ArchbaseActionSynchronizationService(actionRepository, resourceRepository,
                dependencyService);
        ReflectionTestUtils.setField(service, "syncMode", "apply");
        when(actionRepository.findAll(any(com.querydsl.core.types.Predicate.class))).thenReturn(List.of());
        when(resourceRepository.findAll(any(com.querydsl.core.types.Predicate.class))).thenReturn(List.of());
    }

    @Nested
    @DisplayName("herança de recurso")
    class HerancaDeRecurso {

        @Test
        @DisplayName("o recurso vem de @ArchbaseResource quando o método não declara")
        void herdaDaClasse() throws Exception {
            assertThat(recursoResolvido(ControllerComRecursoNaClasse.class, "listar"))
                    .isEqualTo("tms.ordemservico");
        }

        @Test
        @DisplayName("o recurso declarado no método vence o da classe")
        void metodoVenceClasse() throws Exception {
            assertThat(recursoResolvido(ControllerComRecursoNaClasse.class, "instalarPneu"))
                    .isEqualTo("tms.pneu");
        }

        @Test
        @DisplayName("sem recurso em lugar nenhum, devolve nulo — a capacidade não é registrada")
        void semRecursoNenhum() throws Exception {
            // Gravar uma linha de catálogo com nome vazio produziria uma capacidade que ninguém
            // consegue conceder depois. Melhor não registrar e avisar apontando o método.
            assertThat(recursoResolvido(ControllerSemRecurso.class, "semRecurso")).isNull();
        }

        @Test
        @DisplayName("o recurso vem da subclasse concreta quando o método é herdado")
        void herdaDaSubclasseConcreta() throws Exception {
            // Fecha a assimetria com o interceptador, que parte da classe ALVO do proxy — a
            // concreta — e enxerga o @ArchbaseParticularResource posto ali. A varredura partia da
            // classe que DECLARA o método e não enxergava nada: a capacidade era exigida em runtime
            // e nunca catalogada, então ninguém conseguia concedê-la.
            org.reflections.Reflections reflections = mock(org.reflections.Reflections.class);
            when(reflections.getTypesAnnotatedWith(ArchbaseResource.class))
                    .thenReturn(java.util.Set.of(ControllerConcreto.class));
            ReflectionTestUtils.setField(service, "reflections", reflections);

            assertThat(recursoResolvido(ControllerAbstrato.class, "aprovar"))
                    .isEqualTo("tms.abastecimento");
        }

        @Test
        @DisplayName("duas subclasses com recursos diferentes é ambiguidade — não resolve")
        void subclassesAmbiguasNaoResolvem() throws Exception {
            // Catalogar uma das duas seria registrar a capacidade errada em silêncio.
            org.reflections.Reflections reflections = mock(org.reflections.Reflections.class);
            when(reflections.getTypesAnnotatedWith(ArchbaseResource.class))
                    .thenReturn(java.util.Set.of(ControllerConcreto.class, OutroControllerConcreto.class));
            ReflectionTestUtils.setField(service, "reflections", reflections);

            assertThat(recursoResolvido(ControllerAbstrato.class, "aprovar")).isNull();
        }

        private String recursoResolvido(Class<?> tipo, String metodo) throws Exception {
            var m = tipo.getMethod(metodo);
            return (String) ReflectionTestUtils.invokeMethod(
                    service, "resolveResourceName", m, m.getAnnotation(HasPermission.class));
        }
    }

    @Nested
    @DisplayName("semente do nível mínimo")
    class SementeDoNivel {

        @Test
        @DisplayName("minimumLevel da anotação é gravado na ação nova")
        void semeiaONivel() throws Exception {
            ResourceEntity recurso = recurso("tms.ordemservico");
            when(actionRepository.findByActionNameAndResourceName(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction",
                    "aprovar_custo", recurso, metadados("Aprovar custo", AccessLevel.SUPERVISOR));

            ArgumentCaptor<ActionEntity> captor = ArgumentCaptor.forClass(ActionEntity.class);
            verify(actionRepository).save(captor.capture());
            assertThat(captor.getValue().getMinimumLevel()).isEqualTo(AccessLevel.SUPERVISOR);
        }

        @Test
        @DisplayName("NONE na anotação vira nulo na coluna — ausência de piso")
        void noneViraNulo() throws Exception {
            var m = ControllerComRecursoNaClasse.class.getMethod("listar");
            AccessLevel resolvido = (AccessLevel) ReflectionTestUtils.invokeMethod(
                    service, "minimumLevelOf", m.getAnnotation(HasPermission.class));

            assertThat(resolvido).isNull();
        }

        @Test
        @DisplayName("o nível NÃO é sobrescrito em ação já existente — quem manda é o admin")
        void naoSobrescreveOAdmin() throws Exception {
            // Mesma regra da descrição: o código semeia no primeiro registro, e a partir daí a
            // operação ajusta sem deploy. Sobrescrever a cada subida devolveria o controle ao
            // desenvolvedor e tornaria a coluna do admin decorativa.
            ResourceEntity recurso = recurso("tms.ordemservico");
            ActionEntity existente = ActionEntity.builder()
                    .id("action-1").name("aprovar_custo").description("Aprovar custo")
                    .resource(recurso).active(true).minimumLevel(AccessLevel.OPERATOR).build();
            when(actionRepository.findByActionNameAndResourceName(anyString(), anyString()))
                    .thenReturn(Optional.of(existente));

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction",
                    "aprovar_custo", recurso, metadados("Aprovar custo", AccessLevel.TENANT_ADMIN));

            assertThat(existente.getMinimumLevel()).isEqualTo(AccessLevel.OPERATOR);
            verify(actionRepository, never()).save(any(ActionEntity.class));
        }
    }

    @Nested
    @DisplayName("rótulo e categoria")
    class RotuloECategoria {

        @Test
        @DisplayName("rótulo e categoria da anotação nascem com a capacidade")
        void semeiaNaCriacao() {
            ResourceEntity recurso = recurso("tms.ordemservico");
            when(actionRepository.findByActionNameAndResourceName(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(actionRepository.save(any(ActionEntity.class)))
                    .thenAnswer(chamada -> chamada.getArgument(0));

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction", "aprovar_custo", recurso,
                    new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                            "Aprovar o custo da OS", "Aprovar custo", "Custos", null));

            ArgumentCaptor<ActionEntity> captor = ArgumentCaptor.forClass(ActionEntity.class);
            verify(actionRepository).save(captor.capture());
            assertThat(captor.getValue().getLabel()).isEqualTo("Aprovar custo");
            assertThat(captor.getValue().getCategory()).isEqualTo("Custos");
        }

        @Test
        @DisplayName("capacidade existente SEM rótulo recebe o que o código declara")
        void semeiaOQueNuncaFoiSemeado() {
            // Campo novo: nulo numa capacidade existente é ausência do campo na versão em que a
            // linha nasceu, não escolha de quem administra.
            ResourceEntity recurso = recurso("tms.ordemservico");
            ActionEntity existente = existente(recurso, null, null);

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction", "aprovar_custo", recurso,
                    new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                            "Aprovar o custo da OS", "Aprovar custo", "Custos", null));

            assertThat(existente.getLabel()).isEqualTo("Aprovar custo");
            assertThat(existente.getCategory()).isEqualTo("Custos");
        }

        @Test
        @DisplayName("rótulo já gravado NÃO é sobrescrito — quem manda é o admin")
        void naoSobrescreveOAdmin() {
            ResourceEntity recurso = recurso("tms.ordemservico");
            ActionEntity existente = existente(recurso, "Rótulo do admin", "Categoria do admin");

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction", "aprovar_custo", recurso,
                    new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                            "Aprovar o custo da OS", "Aprovar custo", "Custos", null));

            assertThat(existente.getLabel()).isEqualTo("Rótulo do admin");
            assertThat(existente.getCategory()).isEqualTo("Categoria do admin");
            verify(actionRepository, never()).save(any(ActionEntity.class));
        }
    }

    @Nested
    @DisplayName("modo refresh")
    class ModoRefresh {

        @BeforeEach
        void ligarRefresh() {
            ReflectionTestUtils.setField(service, "syncMode", "refresh");
        }

        @Test
        @DisplayName("reescreve descrição, rótulo e categoria a partir do código")
        void reescreveOsTextos() {
            // A saída do beco sem saída: um catálogo que nasceu com centenas de "Criar X" geradas em
            // massa nunca melhora enquanto a semente só semear uma vez.
            ResourceEntity recurso = recurso("tms.ordemservico");
            ActionEntity existente = existente(recurso, "Rótulo antigo", "Categoria antiga");
            existente.setDescription("Criar Ordem de serviço");

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction", "aprovar_custo", recurso,
                    new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                            "Aprovar o custo da OS", "Aprovar custo", "Custos", null));

            assertThat(existente.getDescription()).isEqualTo("Aprovar o custo da OS");
            assertThat(existente.getLabel()).isEqualTo("Aprovar custo");
            assertThat(existente.getCategory()).isEqualTo("Custos");
        }

        @Test
        @DisplayName("atributo removido do código LIMPA o valor — o modo não mente sobre o que faz")
        void removerDoCodigoLimpa() {
            ResourceEntity recurso = recurso("tms.ordemservico");
            ActionEntity existente = existente(recurso, "Rótulo antigo", "Categoria antiga");

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction", "aprovar_custo", recurso,
                    new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                            "Aprovar custo", null, null, null));

            assertThat(existente.getLabel()).isNull();
            assertThat(existente.getCategory()).isNull();
        }

        @Test
        @DisplayName("o NÍVEL MÍNIMO não é tocado — é o único que muda uma decisão")
        void naoTocaNoPiso() {
            // Dos quatro campos semeados, minimumLevel é o único que altera autorização.
            // Reescrevê-lo junto com um rótulo seria mudança de segurança disfarçada de ajuste de
            // texto.
            ResourceEntity recurso = recurso("tms.ordemservico");
            ActionEntity existente = existente(recurso, null, null);
            existente.setMinimumLevel(AccessLevel.OPERATOR);

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction", "aprovar_custo", recurso,
                    new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                            "Aprovar custo", "Aprovar", null, AccessLevel.TENANT_ADMIN));

            assertThat(existente.getMinimumLevel()).isEqualTo(AccessLevel.OPERATOR);
        }

        @Test
        @DisplayName("sem nada a trocar, não grava — subida em refresh não vira escrita em massa")
        void semMudancaNaoGrava() {
            ResourceEntity recurso = recurso("tms.ordemservico");
            ActionEntity existente = existente(recurso, "Aprovar custo", "Custos");
            existente.setDescription("Aprovar o custo da OS");

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction", "aprovar_custo", recurso,
                    new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                            "Aprovar o custo da OS", "Aprovar custo", "Custos", null));

            verify(actionRepository, never()).save(any(ActionEntity.class));
        }
    }

    @Nested
    @DisplayName("modo relatório")
    class ModoRelatorio {

        @BeforeEach
        void ligarRelatorio() {
            ReflectionTestUtils.setField(service, "syncMode", "report");
        }

        @Test
        @DisplayName("não cria recurso")
        void naoCriaRecurso() {
            when(resourceRepository.findByName(anyString())).thenReturn(null);

            ReflectionTestUtils.invokeMethod(service, "ensureResourceExists",
                    "tms.ordemservico", metodoQualquer());

            verify(resourceRepository, never()).save(any(ResourceEntity.class));
        }

        @Test
        @DisplayName("não cria ação")
        void naoCriaAcao() {
            when(actionRepository.findByActionNameAndResourceName(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            ReflectionTestUtils.invokeMethod(service, "synchronizeAction",
                    "aprovar_custo", recurso("tms.ordemservico"),
                    metadados("Aprovar custo", AccessLevel.SUPERVISOR));

            verify(actionRepository, never()).save(any(ActionEntity.class));
        }

        @Test
        @DisplayName("NÃO desativa nada — é a razão de o modo existir")
        void naoDesativa() {
            // O incidente que este modo previne: uma aplicação que ainda não anotou nada sobe, a
            // varredura não encontra capacidade alguma, e o catálogo de tipo API é desativado
            // inteiro. Em report, o mesmo cenário só produz log.
            ResourceEntity recurso = recurso("api.legado");
            ActionEntity acao = ActionEntity.builder()
                    .id("action-1").name("view").description("Ver").resource(recurso).active(true).build();

            when(actionRepository.findAll(any(com.querydsl.core.types.Predicate.class)))
                    .thenReturn(List.of(acao));
            when(resourceRepository.findAll(any(com.querydsl.core.types.Predicate.class)))
                    .thenReturn(List.of(recurso));
            ReflectionTestUtils.setField(service, "reflections", reflectionsVazio());

            ReflectionTestUtils.invokeMethod(service, "disableUnusedActionsAndResources");

            assertThat(acao.getActive()).isTrue();
            assertThat(recurso.getActive()).isTrue();
            verify(actionRepository, never()).save(any(ActionEntity.class));
            verify(resourceRepository, never()).save(any(ResourceEntity.class));
        }

        @Test
        @DisplayName("em apply, o mesmo cenário desativa")
        void emApplyDesativa() {
            ReflectionTestUtils.setField(service, "syncMode", "apply");

            ResourceEntity recurso = recurso("api.legado");
            ActionEntity acao = ActionEntity.builder()
                    .id("action-1").name("view").description("Ver").resource(recurso).active(true).build();

            when(actionRepository.findAll(any(com.querydsl.core.types.Predicate.class)))
                    .thenReturn(List.of(acao));
            when(resourceRepository.findAll(any(com.querydsl.core.types.Predicate.class)))
                    .thenReturn(List.of(recurso));
            ReflectionTestUtils.setField(service, "reflections", reflectionsVazio());

            ReflectionTestUtils.invokeMethod(service, "disableUnusedActionsAndResources");

            assertThat(acao.getActive()).isFalse();
            assertThat(recurso.getActive()).isFalse();
        }
    }

    // ------------------------------------------------------------------ apoio

    private static ResourceEntity recurso(String nome) {
        return ResourceEntity.builder()
                .id("resource-" + nome).name(nome).description("Recurso " + nome).active(true).build();
    }

    private java.lang.reflect.Method metodoQualquer() {
        try {
            return ControllerComRecursoNaClasse.class.getMethod("listar");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Varredura que não encontra nenhuma capacidade — o estado de quem ainda não anotou nada. */
    private org.reflections.Reflections reflectionsVazio() {
        org.reflections.Reflections reflections = mock(org.reflections.Reflections.class);
        when(reflections.getMethodsAnnotatedWith(HasPermission.class)).thenReturn(java.util.Set.of());
        return reflections;
    }

    private ArchbaseActionSynchronizationService.MetadadosDaCapacidade metadados(
            String description, AccessLevel minimumLevel) {
        return new ArchbaseActionSynchronizationService.MetadadosDaCapacidade(
                description, null, null, minimumLevel);
    }

    private ActionEntity existente(ResourceEntity recurso, String label, String category) {
        ActionEntity existente = ActionEntity.builder()
                .id("action-1").name("aprovar_custo").description("Aprovar custo")
                .label(label).category(category)
                .resource(recurso).active(true).build();
        when(actionRepository.findByActionNameAndResourceName(anyString(), anyString()))
                .thenReturn(Optional.of(existente));
        when(actionRepository.save(any(ActionEntity.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));
        return existente;
    }
}
