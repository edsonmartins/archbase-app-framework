package br.com.archbase.security.service;

import br.com.archbase.security.domain.dto.CapabilityDependencyNodeDto;
import br.com.archbase.security.domain.dto.CapabilityDependencyTreeDto;
import br.com.archbase.security.domain.entity.DependencySource;
import br.com.archbase.security.persistence.ActionDependencyEntity;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.repository.ActionDependencyJpaRepository;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.util.CapabilityRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mantém as arestas de dependência entre capacidades — a Camada 3 do plano da tela de permissões.
 *
 * <p>Uma aresta declara <i>a origem não serve para nada sem o alvo</i>, e cobre as duas perguntas
 * que a tela não sabia responder: "do que esta permissão depende?" e "qual endpoint esta tela usa?".
 * Ver {@code CONTRATO_DEPENDENCIAS_DE_CAPACIDADE.md}.
 *
 * <h2>Arestas são propriedade do código</h2>
 *
 * <p>É o que torna esta reconciliação simples. Descrição e {@code minimumLevel} são semeados pelo
 * código no primeiro registro e a partir dali pertencem ao administrador — daí a sincronização nunca
 * poder sobrescrevê-los, e daí o problema conhecido de "as ações já coletadas precisariam ser
 * atualizadas ao menos uma vez". Aresta não tem esse dilema: ela é um fato sobre o código, não uma
 * configuração. É refeita a cada subida, e nenhum mecanismo de refresh é necessário.
 *
 * <h2>Nunca decide nada</h2>
 *
 * <p>O {@code ArchbaseAccessEvaluator} não lê esta tabela. Não há sexto portão e não há flag que
 * crie um: se {@code aprovar_custo} passasse a exigir {@code view} na decisão, toda instalação
 * existente perderia acesso na primeira subida após a atualização — em silêncio, porque ninguém
 * declarou aquelas arestas pensando em autorização.
 */
@Service
@Slf4j
public class ArchbaseCapabilityDependencyService {

    private final ActionDependencyJpaRepository dependencyRepository;
    private final ActionJpaRepository actionRepository;

    public ArchbaseCapabilityDependencyService(ActionDependencyJpaRepository dependencyRepository,
                                               ActionJpaRepository actionRepository) {
        this.dependencyRepository = dependencyRepository;
        this.actionRepository = actionRepository;
    }

    /**
     * O resultado de interpretar as dependências declaradas — o que vale, e o que foi descartado.
     *
     * <p><b>Uma leitura só, compartilhada pelos dois coletores.</b> A varredura de
     * {@code @HasPermission} e o registro de tela declaram no mesmo formato, e duas implementações
     * da mesma interpretação já produziram, neste módulo, o defeito de catalogar com um nome e
     * consultar com outro. O que cada lado faz de diferente é apenas <b>como avisa</b>: a varredura
     * aponta o método, o registro aponta o payload.
     *
     * @param validas         referências qualificadas, prontas para virar aresta
     * @param invalidas       o que não pôde ser interpretado, na forma original
     * @param autoReferencias a capacidade declarando dependência de si mesma — redundância, não erro
     */
    public record Qualificadas(Set<String> validas, List<String> invalidas, List<String> autoReferencias) {
    }

    /**
     * Interpreta as dependências declaradas, descartando o que não vale.
     *
     * <p>Descarta em vez de lançar: uma dependência mal declarada não pode impedir o catálogo de ser
     * sincronizado, nem derrubar a subida da aplicação. Quem chama registra o aviso.
     *
     * @param recursoDeQuemDeclara completa a forma curta ({@code "view"})
     * @param acaoDeQuemDeclara    para detectar a dependência de si mesma
     */
    public static Qualificadas qualificar(Collection<String> declaradas, String recursoDeQuemDeclara,
                                          String acaoDeQuemDeclara) {
        Set<String> validas = new LinkedHashSet<>();
        List<String> invalidas = new ArrayList<>();
        List<String> autoReferencias = new ArrayList<>();

        if (declaradas == null) {
            return new Qualificadas(validas, invalidas, autoReferencias);
        }

        String propria = CapabilityRef.de(recursoDeQuemDeclara, acaoDeQuemDeclara);

        for (String declarada : declaradas) {
            String qualificada = CapabilityRef.qualificar(declarada, recursoDeQuemDeclara);
            if (qualificada == null) {
                invalidas.add(declarada);
            } else if (qualificada.equals(propria)) {
                autoReferencias.add(qualificada);
            } else {
                validas.add(qualificada);
            }
        }
        return new Qualificadas(validas, invalidas, autoReferencias);
    }

    /**
     * Reconcilia <b>integralmente</b> as arestas declaradas em {@code @HasPermission}.
     *
     * <p>Integral porque a varredura tem a lista completa do que o código declara — a mesma premissa
     * que autoriza a desativação de capacidades. Quem chama é responsável por <b>não</b> chamar
     * quando a lista está incompleta: reconciliar a partir de lista parcial removeria arestas
     * válidas, pela mesma razão que podar a partir dela removeria capacidades válidas.
     *
     * @param declaradas capacidade → conjunto de referências já qualificadas ({@code recurso:acao})
     * @param somenteRelatorio registra o que faria, sem escrever nada
     */
    @Transactional
    public void reconcileScan(Map<ActionEntity, Set<String>> declaradas, boolean somenteRelatorio) {
        Map<String, ActionEntity> catalogo = catalogoPorCapacidade();

        Map<String, ActionEntity> origens = new HashMap<>();
        Map<String, Set<String>> desejadasPorAcao = new HashMap<>();
        declaradas.forEach((acao, capacidades) -> {
            origens.put(acao.getId(), acao);
            desejadasPorAcao.put(acao.getId(), capacidades);
        });

        List<ActionDependencyEntity> existentes = dependencyRepository.findAllDeclaredBy(DependencySource.SCAN);
        Map<String, List<ActionDependencyEntity>> existentesPorAcao = existentes.stream()
                .filter(d -> d.getAction() != null)
                .collect(Collectors.groupingBy(d -> d.getAction().getId()));

        List<ActionDependencyEntity> aCriar = new ArrayList<>();
        List<ActionDependencyEntity> aRemover = new ArrayList<>();
        List<ActionDependencyEntity> aResolver = new ArrayList<>();

        desejadasPorAcao.forEach((actionId, desejadas) -> {
            List<ActionDependencyEntity> daAcao = existentesPorAcao.getOrDefault(actionId, List.of());
            Set<String> jaGravadas = daAcao.stream()
                    .map(ActionDependencyEntity::getRequiredCapability)
                    .collect(Collectors.toSet());

            desejadas.stream()
                    .filter(capacidade -> !jaGravadas.contains(capacidade))
                    .forEach(capacidade -> aCriar.add(
                            nova(origens.get(actionId), capacidade, DependencySource.SCAN, catalogo)));

            daAcao.stream()
                    .filter(d -> !desejadas.contains(d.getRequiredCapability()))
                    .forEach(aRemover::add);

            daAcao.stream()
                    .filter(d -> desejadas.contains(d.getRequiredCapability()))
                    .filter(d -> reresolver(d, catalogo))
                    .forEach(aResolver::add);
        });

        // Aresta cuja capacidade de origem não declara mais nada — ou deixou de existir no código.
        existentesPorAcao.forEach((actionId, daAcao) -> {
            if (!desejadasPorAcao.containsKey(actionId)) {
                aRemover.addAll(daAcao);
            }
        });

        aplicar(aCriar, aRemover, aResolver, somenteRelatorio, "@HasPermission");
    }

    /**
     * Reconcilia as arestas de <b>uma</b> capacidade declarada por uma tela.
     *
     * <p>Escopada de propósito. Um recurso pode ser declarado por mais de uma tela, e cada uma envia
     * só as ações que usa — foi o que tornou o registro de tela aditivo em primeiro lugar. Ação
     * ausente do payload não é tocada; a chamada nem acontece para ela.
     *
     * @param capacidades {@code null} significa <b>não declarei</b> e não toca em nada — é o que todo
     *                    cliente anterior envia. Conjunto vazio significa <b>declaro que não há
     *                    nenhuma</b>, e remove as que existirem.
     */
    @Transactional
    public void reconcileRegister(ActionEntity acao, Collection<String> capacidades) {
        if (acao == null || capacidades == null) {
            return;
        }

        Set<String> desejadas = new LinkedHashSet<>(capacidades);
        List<ActionDependencyEntity> existentes =
                dependencyRepository.findByActionIdAndDeclaredBy(acao.getId(), DependencySource.REGISTER);
        Set<String> jaGravadas = existentes.stream()
                .map(ActionDependencyEntity::getRequiredCapability)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!existentes.isEmpty() && !jaGravadas.equals(desejadas)) {
            // Duas telas declarando a mesma capacidade com dependências diferentes alternam a cada
            // abertura. É defeito de quem declarou — as duas estão discordando sobre o que aquela
            // capacidade é —, e sem este aviso a alternância seria invisível.
            log.warn("Dependências da capacidade '{}:{}' substituídas por um registro de tela: "
                            + "estava {}, passou a {}. Se duas telas declaram esta ação com "
                            + "'requires' diferentes, elas vão se sobrescrever a cada abertura.",
                    acao.getResource() == null ? "?" : acao.getResource().getName(), acao.getName(),
                    jaGravadas, desejadas);
        }

        Map<String, ActionEntity> catalogo = catalogoPorCapacidade();

        List<ActionDependencyEntity> aCriar = desejadas.stream()
                .filter(capacidade -> !jaGravadas.contains(capacidade))
                .map(capacidade -> nova(acao, capacidade, DependencySource.REGISTER, catalogo))
                .toList();

        List<ActionDependencyEntity> aRemover = existentes.stream()
                .filter(d -> !desejadas.contains(d.getRequiredCapability()))
                .toList();

        List<ActionDependencyEntity> aResolver = existentes.stream()
                .filter(d -> desejadas.contains(d.getRequiredCapability()))
                .filter(d -> reresolver(d, catalogo))
                .toList();

        aplicar(aCriar, aRemover, aResolver, false, "registro de tela");
    }

    /**
     * Como {@link #reconcileRegister(ActionEntity, Collection)}, a partir do identificador.
     *
     * <p>Existe porque o registro de tela atravessa DTO e adaptador, e não tem a entidade em mãos.
     * Capacidade inexistente é ignorada em silêncio: a ação acabou de ser criada no mesmo fluxo, e
     * se não está lá é porque a criação falhou — o erro pertence a esse passo, não a este.
     */
    @Transactional
    public void reconcileRegisterById(String actionId, Collection<String> capacidades) {
        if (actionId == null || capacidades == null) {
            return;
        }
        actionRepository.findById(actionId)
                .ifPresent(acao -> reconcileRegister(acao, capacidades));
    }

    /**
     * Reaponta as arestas pendentes cujo alvo já entrou no catálogo.
     *
     * <p>É o que faz uma dependência declarada antes do alvo passar a valer sozinha — sem
     * reprocessamento, sem ordem imposta à varredura.
     */
    @Transactional
    public int resolvePending() {
        List<ActionDependencyEntity> pendentes = dependencyRepository.findUnresolved();
        if (pendentes.isEmpty()) {
            return 0;
        }
        Map<String, ActionEntity> catalogo = catalogoPorCapacidade();
        List<ActionDependencyEntity> resolvidas = pendentes.stream()
                .filter(d -> reresolver(d, catalogo))
                .toList();
        if (!resolvidas.isEmpty()) {
            dependencyRepository.saveAll(resolvidas);
            log.info("{} dependência(s) que estavam pendentes foram resolvidas — o alvo entrou no catálogo.",
                    resolvidas.size());
        }
        return resolvidas.size();
    }

    /**
     * O teto de profundidade do fecho transitivo.
     *
     * <p>Não existe para conter ciclo — o conjunto de visitados já faz isso, e ciclos são
     * declarações possíveis e às vezes corretas. Existe para conter <b>cadeia longa</b>: uma
     * dependência com trinta níveis produz uma resposta que ninguém lê e um percurso que ninguém
     * pediu. Quando o teto é atingido, a resposta diz.
     */
    private static final int PROFUNDIDADE_MAXIMA = 10;

    /**
     * Tudo de que uma capacidade depende, direta e indiretamente.
     *
     * <p>Percurso em largura com conjunto de visitados. <b>Ciclo não trava</b>: {@code A → B} e
     * {@code B → A} é uma declaração possível, e o visitado encerra o ramo em vez de o percurso
     * girar. Capacidade não resolvida entra na resposta marcada e não é expandida — não há por onde
     * seguir a partir do que não existe.
     */
    @Transactional(readOnly = true)
    public Optional<CapabilityDependencyTreeDto> closureOf(String actionId) {
        Optional<ActionEntity> partida = actionRepository.findById(actionId);
        if (partida.isEmpty() || partida.get().getResource() == null) {
            return Optional.empty();
        }

        String raiz = CapabilityRef.de(partida.get().getResource().getName(), partida.get().getName());
        Map<String, ActionEntity> catalogo = catalogoPorCapacidade();
        Map<String, List<String>> arestas = arestasPorOrigem();

        List<CapabilityDependencyNodeDto> encontradas = new ArrayList<>();
        Set<String> visitadas = new LinkedHashSet<>();
        visitadas.add(raiz);

        // (capacidade, quem a exigiu) — a origem de cada nível vem junto para que a resposta explique
        // o caminho, e não apenas o destino.
        List<String[]> nivel = new ArrayList<>();
        for (String alvo : arestas.getOrDefault(raiz, List.of())) {
            nivel.add(new String[]{alvo, raiz});
        }

        boolean truncado = false;
        int profundidade = 1;

        while (!nivel.isEmpty()) {
            if (profundidade > PROFUNDIDADE_MAXIMA) {
                truncado = true;
                break;
            }

            List<String[]> proximo = new ArrayList<>();
            for (String[] passo : nivel) {
                String capacidade = passo[0];
                if (!visitadas.add(capacidade)) {
                    continue;
                }

                ActionEntity alvo = catalogo.get(capacidade);
                encontradas.add(no(capacidade, alvo, profundidade, passo[1]));

                if (alvo == null) {
                    // Não existe: não há de onde continuar.
                    continue;
                }
                arestas.getOrDefault(capacidade, List.of())
                        .forEach(seguinte -> proximo.add(new String[]{seguinte, capacidade}));
            }

            nivel = proximo;
            profundidade++;
        }

        encontradas.sort(Comparator
                .comparingInt(CapabilityDependencyNodeDto::getDepth)
                .thenComparing(CapabilityDependencyNodeDto::getCapability));

        return Optional.of(CapabilityDependencyTreeDto.builder()
                .actionId(actionId)
                .capability(raiz)
                .dependencies(encontradas)
                .truncated(truncado)
                .build());
    }

    private CapabilityDependencyNodeDto no(String capacidade, ActionEntity alvo, int profundidade,
                                           String exigidaPor) {
        return CapabilityDependencyNodeDto.builder()
                .capability(capacidade)
                .actionId(alvo == null ? null : alvo.getId())
                .resourceName(CapabilityRef.recursoDe(capacidade))
                .actionName(CapabilityRef.acaoDe(capacidade))
                .actionDescription(alvo == null ? null : alvo.getDescription())
                .depth(profundidade)
                .requiredBy(exigidaPor)
                .resolved(alvo != null)
                .build();
    }

    /** Todas as arestas indexadas pela capacidade de origem, em texto. */
    private Map<String, List<String>> arestasPorOrigem() {
        Map<String, List<String>> porOrigem = new LinkedHashMap<>();
        for (ActionDependencyEntity aresta : dependencyRepository.findAllWithOrigin()) {
            ActionEntity origem = aresta.getAction();
            if (origem == null || origem.getResource() == null) {
                continue;
            }
            porOrigem.computeIfAbsent(
                            CapabilityRef.de(origem.getResource().getName(), origem.getName()),
                            c -> new ArrayList<>())
                    .add(aresta.getRequiredCapability());
        }
        return porOrigem;
    }

    /** Quantas arestas apontam para capacidade que não existe no catálogo. */
    @Transactional(readOnly = true)
    public long countUnresolved() {
        return dependencyRepository.countUnresolved();
    }

    // ── interno ──────────────────────────────────────────────────────────────────────────────

    /** Todo o catálogo indexado por {@code recurso:acao}, para traduzir o alvo de cada aresta. */
    private Map<String, ActionEntity> catalogoPorCapacidade() {
        Map<String, ActionEntity> porCapacidade = new HashMap<>();
        for (ActionEntity acao : actionRepository.findAllWithResource()) {
            if (acao.getResource() == null || acao.getName() == null) {
                continue;
            }
            // Duplicata de (recurso, ação) é ambiguidade que o validador de subida já reporta; aqui
            // a primeira vence, de forma determinística, em vez de a resolução variar por execução.
            porCapacidade.putIfAbsent(
                    CapabilityRef.de(acao.getResource().getName(), acao.getName()), acao);
        }
        return porCapacidade;
    }

    private ActionDependencyEntity nova(ActionEntity origem, String capacidade,
                                        DependencySource declaredBy, Map<String, ActionEntity> catalogo) {
        ActionDependencyEntity aresta = new ActionDependencyEntity();
        aresta.setAction(origem);
        aresta.setRequiredCapability(capacidade);
        aresta.setRequiredAction(catalogo.get(capacidade));
        aresta.setDeclaredBy(declaredBy);
        aresta.setCreateEntityDate(LocalDateTime.now());
        aresta.setCreatedByUser("archbase");
        return aresta;
    }

    /** {@code true} quando o alvo resolvido mudou e a linha precisa ser gravada. */
    private boolean reresolver(ActionDependencyEntity aresta, Map<String, ActionEntity> catalogo) {
        ActionEntity alvo = catalogo.get(aresta.getRequiredCapability());
        String atual = aresta.getRequiredAction() == null ? null : aresta.getRequiredAction().getId();
        String novo = alvo == null ? null : alvo.getId();
        if (Objects.equals(atual, novo)) {
            return false;
        }
        aresta.setRequiredAction(alvo);
        aresta.setUpdateEntityDate(LocalDateTime.now());
        aresta.setLastModifiedByUser("archbase");
        return true;
    }

    private void aplicar(List<ActionDependencyEntity> aCriar, List<ActionDependencyEntity> aRemover,
                         List<ActionDependencyEntity> aResolver, boolean somenteRelatorio, String origem) {
        if (aCriar.isEmpty() && aRemover.isEmpty() && aResolver.isEmpty()) {
            return;
        }

        if (somenteRelatorio) {
            log.warn("[report] dependências ({}): criaria {}, removeria {}. Criaria: {}. Removeria: {}.",
                    origem, aCriar.size(), aRemover.size(), rotulos(aCriar), rotulos(aRemover));
            return;
        }

        if (!aRemover.isEmpty()) {
            dependencyRepository.deleteAll(aRemover);
        }
        if (!aCriar.isEmpty()) {
            dependencyRepository.saveAll(aCriar);
        }
        if (!aResolver.isEmpty()) {
            dependencyRepository.saveAll(aResolver);
        }

        long pendentes = aCriar.stream().filter(ActionDependencyEntity::isUnresolved).count();
        log.info("Dependências ({}): {} criada(s), {} removida(s), {} reapontada(s).{}",
                origem, aCriar.size(), aRemover.size(), aResolver.size(),
                pendentes == 0 ? "" : " " + pendentes
                        + " apontam para capacidade que ainda não está no catálogo — "
                        + "ficam pendentes e são resolvidas quando o alvo aparecer.");
    }

    private List<String> rotulos(List<ActionDependencyEntity> arestas) {
        return arestas.stream()
                .map(d -> (d.getAction() == null || d.getAction().getResource() == null
                        ? "?" : d.getAction().getResource().getName() + ":" + d.getAction().getName())
                        + " -> " + d.getRequiredCapability())
                .toList();
    }
}
