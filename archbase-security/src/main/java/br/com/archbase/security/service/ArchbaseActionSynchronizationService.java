package br.com.archbase.security.service;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.annotation.ArchbaseResource;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.QActionEntity;
import br.com.archbase.security.persistence.QResourceEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.util.AuthorizationAnnotationUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.List;

/**
 * Sincroniza o catálogo com o que o código declara em {@code @HasPermission}.
 *
 * <p>Cria recurso e ação que faltam, reativa o que voltou a existir e <b>desativa o que não é mais
 * declarado</b>. É essa última parte que exige cuidado: quando a varredura roda numa aplicação que
 * ainda não anotou nada, ela não encontra nenhuma capacidade e desativa todo o catálogo de tipo
 * {@code API} — foi exatamente o que aconteceu no gestor-rq, onde 8 recursos criados por seed foram
 * desativados por {@code archbase} sem que ninguém tivesse pedido.
 *
 * <p>Daí o modo relatório: com {@code archbase.security.sync.mode=report} a varredura <b>apenas
 * registra em log</b> o que faria, sem escrever nada. É o passo que se roda antes de ligar o
 * primeiro {@code @HasPermission} num sistema em produção.
 */
@Service
@Slf4j
public class ArchbaseActionSynchronizationService {

    /** Registra o que faria, sem escrever nada. */
    private static final String MODE_REPORT = "report";

    /** Escreve, e ainda RESSEMEIA os textos das capacidades existentes a partir do código. */
    private static final String MODE_REFRESH = "refresh";

    private final ActionJpaRepository actionRepository; // Repositório de ações
    private final ResourceJpaRepository resourceRepository; // Repositório de recursos

    private Reflections reflections;

    @Value("${archbase.security.scan-packages:}")
    private String scanPackages;

    /**
     * {@code apply} (padrão) escreve; {@code report} apenas registra o que faria.
     *
     * <p>O padrão continua sendo escrever, para não mudar comportamento de quem já depende da
     * sincronização. Quem está prestes a anotar o primeiro endpoint de um sistema existente deve
     * rodar em {@code report} antes, ler o log e só então voltar para {@code apply}.
     */
    @Value("${archbase.security.sync.mode:apply}")
    private String syncMode;


    private final ArchbaseCapabilityDependencyService dependencyService;

    public ArchbaseActionSynchronizationService(ActionJpaRepository actionRepository,
                                                ResourceJpaRepository resourceJpaRepository,
                                                ArchbaseCapabilityDependencyService dependencyService) {
        this.actionRepository = actionRepository;
        this.resourceRepository = resourceJpaRepository;
        this.dependencyService = dependencyService;
    }

    /**
     * Tenant usado pela varredura, que roda na subida e portanto fora de qualquer requisição.
     *
     * <p>Sem isto, {@code archbase.app.tenant.fail-on-missing=true} — recomendado no guia de
     * endurecimento — <b>impedia a aplicação de subir</b>: o resolver de tenant recusa qualquer
     * sessão aberta sem contexto, e esta varredura abre uma. A flag existe para recusar
     * <i>requisição</i> sem tenant, não para proibir trabalho de sistema.
     */
    @Value("${archbase.app.tenant.default.id:archbase}")
    private String tenantDaVarredura;

    @PostConstruct
    public void initialize() {
        if (StringUtils.isEmpty(scanPackages)) {
            log.warn("Nenhum pacote de varredura especificado para segurança do Archbase. Defina a propriedade 'archbase.security.scan-packages'.");
            return;
        }
        this.reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(scanPackages.split(","))
                .setScanners(Scanners.MethodsAnnotated, Scanners.TypesAnnotated));

        String tenantAnterior = ArchbaseTenantContext.getTenantId();
        boolean definidoAqui = tenantAnterior == null || tenantAnterior.isBlank();
        if (definidoAqui) {
            ArchbaseTenantContext.setTenantId(tenantDaVarredura);
        }
        try {
            synchronizeActionsAndResources();
        } catch (Exception ex) {
            log.error("Não foi possível sincronizar as ações do sistema {}",ex.getMessage());
        } finally {
            // A thread que sobe a aplicação volta para o pool: deixar o tenant gravado nela faria
            // uma requisição futura herdar este valor sem ninguém ter pedido.
            if (definidoAqui) {
                ArchbaseTenantContext.clear();
            }
        }
    }

    /** {@code true} quando a varredura não deve escrever nada. */
    private boolean somenteRelatorio() {
        return MODE_REPORT.equalsIgnoreCase(syncMode);
    }

    /**
     * {@code true} quando os textos das capacidades existentes devem ser reescritos pelo código.
     *
     * <p><b>Existe porque a semente só semeia uma vez.</b> Descrição, rótulo e categoria são
     * gravados no primeiro registro da capacidade e a partir dali pertencem ao admin. A regra é
     * certa no dia a dia e deixa um beco sem saída: um catálogo que já nasceu com centenas de
     * "Criar X" geradas em massa nunca melhora, por mais que o código passe a declarar rótulos
     * bons. Este modo é a saída — usado uma vez, de propósito, e depois desligado.
     *
     * <p><b>Ressemeia texto, nunca o piso.</b> {@code minimumLevel} fica de fora: dos quatro campos
     * semeados, é o único que muda uma <b>decisão</b> de acesso. Um admin que baixou o piso de uma
     * capacidade tinha um motivo, e reescrevê-lo junto com um rótulo seria uma mudança de segurança
     * disfarçada de ajuste de texto.
     */
    private boolean ressemeando() {
        return MODE_REFRESH.equalsIgnoreCase(syncMode);
    }

    /**
     * O que o código declara sobre a capacidade, além do nome.
     *
     * <p>Visível no pacote para que o teste possa exercitar {@code synchronizeAction} diretamente,
     * que é onde moram as regras de semeadura e de ressemeadura.
     */
    record MetadadosDaCapacidade(String description, String label, String category,
                                 AccessLevel minimumLevel) {
    }

    protected void synchronizeActionsAndResources() {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(HasPermission.class);
        boolean houveMetodoSemRecurso = false;
        java.util.Map<ActionEntity, Set<String>> dependenciasDeclaradas = new java.util.LinkedHashMap<>();

        if (somenteRelatorio()) {
            log.warn("archbase.security.sync.mode=report — a sincronização NÃO vai escrever nada. "
                    + "{} método(s) anotado(s) com @HasPermission encontrado(s).", methods.size());
        }

        if (ressemeando()) {
            // Barulhento de propósito: este modo DESCARTA ajuste feito pelo admin na descrição, no
            // rótulo e na categoria. Quem o ligou precisa ver isso no log da subida, e quem o
            // esqueceu ligado precisa ver a cada subida.
            log.warn("archbase.security.sync.mode=refresh — descrição, rótulo e categoria das "
                    + "capacidades existentes serão REESCRITOS a partir do código, descartando "
                    + "ajustes feitos pelo admin. O nível mínimo NÃO é tocado. Volte para 'apply' "
                    + "depois de rodar uma vez.");
        }

        for (Method method : methods) {
            HasPermission permission = method.getAnnotation(HasPermission.class);
            String actionName = permission.action();
            String description = permission.description();
            String resourceName = resolveResourceName(method, permission);

            if (StringUtils.isEmpty(resourceName)) {
                // Sem recurso não há capacidade. Avisa apontando o método, em vez de gravar uma
                // linha de catálogo com nome vazio que ninguém consegue conceder depois.
                // A resolução aqui parte de method.getDeclaringClass(). Com @HasPermission herdado
                // de um controller abstrato e @ArchbaseResource na subclasse concreta, a varredura
                // não enxerga o recurso — enquanto o interceptador, que parte da classe alvo do
                // proxy, enxerga.
                log.error("@HasPermission em {}#{} não declara resource, e a classe que o DECLARA "
                                + "não tem @ArchbaseResource. A capacidade não pôde ser registrada. "
                                + "Se @ArchbaseResource está numa subclasse, mova-a para a classe que "
                                + "declara o método, ou declare resource na própria anotação.",
                        method.getDeclaringClass().getName(), method.getName());
                houveMetodoSemRecurso = true;
                continue;
            }

            ResourceEntity resource = ensureResourceExists(resourceName, method);

            if (resource == null) {
                // Em modo relatório o recurso ainda não existe e nada foi gravado — mas a ação
                // precisa aparecer no inventário mesmo assim. Sem isto, o relatório lista "criaria
                // o recurso X" e nenhuma das ações que viriam com ele, que é justamente a lista que
                // se quer ver antes de rodar em produção.
                if (somenteRelatorio()) {
                    AccessLevel minimo = minimumLevelOf(permission);
                    log.warn("[report] criaria a ação '{}:{}'{} (no recurso que também seria criado)",
                            resourceName, actionName,
                            minimo == null ? "" : " com nível mínimo " + minimo);
                }
                continue;
            }

            ActionEntity acao = synchronizeAction(actionName, resource, new MetadadosDaCapacidade(
                    description,
                    StringUtils.trimToNull(permission.label()),
                    StringUtils.trimToNull(permission.category()),
                    minimumLevelOf(permission)));
            if (acao != null) {
                // Uma capacidade pode ser declarada por mais de um método (sobrecarga de rota, por
                // exemplo). As declaracoes se somam, em vez de a ultima vencer: cada metodo conhece
                // as dependencias do que ele faz, e descartar as do primeiro seria perder metade da
                // informacao por ordem de varredura.
                dependenciasDeclaradas
                        .computeIfAbsent(acao, a -> new java.util.LinkedHashSet<>())
                        .addAll(dependenciasDe(permission, resourceName, method));
            }
        }

        if (houveMetodoSemRecurso) {
            // Desativar exige a lista COMPLETA do que o código declara. Com pelo menos um método
            // sem recurso resolvível, essa lista está incompleta — e desativar a partir dela
            // derrubaria do catálogo capacidades que existem, só não foram reconhecidas. O
            // resultado seria 403 permanente num endpoint que o admin nem consegue mais conceder.
            log.error("Desativação de capacidades e reconciliação de dependências NÃO executadas: "
                    + "há método(s) com @HasPermission cujo recurso não pôde ser resolvido. Corrija "
                    + "os avisos acima primeiro — trabalhar com a lista incompleta removeria "
                    + "capacidades e dependências válidas do catálogo.");
            return;
        }

        // Mesma premissa da poda: a reconciliação das arestas é INTEGRAL, e integral só é seguro
        // com a lista completa do que o código declara. Daí estar depois do mesmo guard.
        dependencyService.reconcileScan(dependenciasDeclaradas, somenteRelatorio());

        if (!somenteRelatorio()) {
            // Alcança também as arestas declaradas por telas, que a reconciliação acima não toca.
            // Uma dependência gravada antes de o alvo existir passa a valer aqui, sozinha — sem
            // reprocessamento e sem impor ordem à varredura.
            dependencyService.resolvePending();
        }

        disableUnusedActionsAndResources();
    }

    /**
     * As dependências que a anotação declara, já qualificadas — as inválidas caem com aviso.
     *
     * <p>Aresta mal declarada não impede o catálogo de ser sincronizado nem derruba a subida: ela é
     * descartada, com o método apontado no log. Recusar a aplicação inteira por causa de um nome de
     * recurso mal escolhido trocaria um defeito de catálogo por uma indisponibilidade.
     */
    private Set<String> dependenciasDe(HasPermission permission, String resourceName, Method method) {
        ArchbaseCapabilityDependencyService.Qualificadas qualificadas =
                ArchbaseCapabilityDependencyService.qualificar(
                        java.util.Arrays.asList(permission.requires()), resourceName, permission.action());

        qualificadas.invalidas().forEach(declarada -> log.error(
                "@HasPermission em {}#{} declara requires=\"{}\", que não é uma capacidade válida. "
                        + "Use \"acao\" para o mesmo recurso ou \"recurso:acao\" para outro; o nome do "
                        + "recurso não pode conter ':'. A dependência foi ignorada.",
                method.getDeclaringClass().getName(), method.getName(), declarada));

        // Redundância, não erro: a capacidade não depende de si mesma, e gravar a aresta faria a
        // tela sugerir conceder o que já está sendo concedido.
        qualificadas.autoReferencias().forEach(capacidade -> log.warn(
                "@HasPermission em {}#{} declara dependência de si mesma ('{}'). Ignorada.",
                method.getDeclaringClass().getName(), method.getName(), capacidade));

        return qualificadas.validas();
    }

    /**
     * O recurso declarado no método, ou o herdado de {@link ArchbaseResource} na classe.
     *
     * <p>Só o recurso é herdado. A ação é sempre por método — herdá-la faria um {@code DELETE}
     * exigir a mesma capacidade de um {@code GET}, o que leria como proteção e seria grosseria.
     */
    private String resolveResourceName(Method method, HasPermission permission) {
        // Mesma função do interceptador que decide. Duas implementações da mesma resolução já
        // produziram o defeito de catalogar com um nome e consultar com outro.
        String pelaDeclarante = AuthorizationAnnotationUtils.resolveResourceName(method, permission.resource());
        if (StringUtils.isNotEmpty(pelaDeclarante)) {
            return pelaDeclarante;
        }
        return resourceDeSubclasseConcreta(method);
    }

    /**
     * O recurso declarado numa <b>subclasse concreta</b> do controller que declara o método.
     *
     * <p>Fecha a assimetria que sobrava entre a varredura e o interceptador. O interceptador parte
     * da classe ALVO do proxy — a concreta — e enxerga um {@code @ArchbaseResource} posto ali. A
     * varredura parte de {@code method.getDeclaringClass()}, e com {@code @HasPermission} herdado
     * de um controller abstrato não enxergava nada: a capacidade era exigida em runtime e nunca
     * catalogada, então ninguém conseguia concedê-la e todo não-administrador levava 403 permanente.
     *
     * <p>Ambíguo é tratado como não resolvido: se mais de uma subclasse concreta declara recursos
     * <b>diferentes</b>, não há como saber qual das capacidades o método representa — e escolher
     * uma catalogaria a errada em silêncio.
     */
    private String resourceDeSubclasseConcreta(Method method) {
        if (reflections == null) {
            return null;
        }
        Class<?> declarante = method.getDeclaringClass();

        Set<String> candidatos = reflections.getTypesAnnotatedWith(ArchbaseResource.class).stream()
                .filter(tipo -> tipo != declarante && declarante.isAssignableFrom(tipo))
                .map(tipo -> tipo.getAnnotation(ArchbaseResource.class).value())
                .filter(StringUtils::isNotEmpty)
                .collect(java.util.stream.Collectors.toSet());

        if (candidatos.size() == 1) {
            return candidatos.iterator().next();
        }
        if (candidatos.size() > 1) {
            log.error("@HasPermission em {}#{} é herdado por {} subclasses com @ArchbaseResource "
                            + "distintos ({}). Não há como saber qual capacidade catalogar — declare "
                            + "resource na própria anotação.",
                    declarante.getName(), method.getName(), candidatos.size(), candidatos);
        }
        return null;
    }

    /** {@code NONE} na anotação é ausência de piso, e vira {@code null} na coluna. */
    private AccessLevel minimumLevelOf(HasPermission permission) {
        return AccessLevel.isUnset(permission.minimumLevel()) ? null : permission.minimumLevel();
    }

    private ResourceEntity ensureResourceExists(String resourceName, Method method) {
        ResourceEntity resource = resourceRepository.findByName(resourceName);
        if (resource == null) {
            if (somenteRelatorio()) {
                log.warn("[report] criaria o recurso '{}' (tipo API)", resourceName);
                return null;
            }
            resource = new ResourceEntity();
            resource.setName(resourceName);
            resource.setDescription(descriptionOf(method, resourceName));
            resource.setCreateEntityDate(LocalDateTime.now());
            resource.setCreatedByUser("archbase");
            resource.setActive(true);
            resource.setType(TipoRecurso.API);
            resourceRepository.save(resource);
        } else {
            if (!resource.getActive()) {
                if (somenteRelatorio()) {
                    log.warn("[report] reativaria o recurso '{}' e o marcaria como tipo API", resourceName);
                    return resource;
                }
                resource.setUpdateEntityDate(LocalDateTime.now());
                resource.setActive(true);
                resource.setLastModifiedByUser("archbase");
                resource.setType(TipoRecurso.API);
                resource = resourceRepository.save(resource);
            } else if (resource.getType() == null) {
                // CLASSIFICACAO RETROATIVA, e so isso.
                //
                // TIPO_RECURSO e nulavel e chegou depois de muita gente ja ter catalogo: recurso
                // criado por versao anterior, ou pelo POST /api/v1/resource sem tipo, ficou sem
                // classificacao nenhuma. A tela de permissoes passa a separar tela de endpoint por
                // este campo, e sem preencher o que ja existe a separacao nasceria vazia justamente
                // nas bases que mais precisam dela.
                //
                // So preenche o NULO. Recurso que ja tem tipo nao e tocado — inclusive um VIEW que
                // por acaso tambem seja declarado em codigo: sobrescrever faria a classificacao
                // oscilar entre a subida da aplicacao e a abertura da tela, e um recurso que alterna
                // de tipo alterna tambem de secao na interface a cada deploy.
                //
                // Nao muda quem e podado: disableUnusedActionsAndResources so desativa o que NAO
                // esta declarado no codigo, e este ramo so alcanca recurso que esta.
                if (somenteRelatorio()) {
                    log.warn("[report] classificaria o recurso '{}' como tipo API (hoje sem tipo)",
                            resourceName);
                    return resource;
                }
                resource.setType(TipoRecurso.API);
                resource.setUpdateEntityDate(LocalDateTime.now());
                resource.setLastModifiedByUser("archbase");
                resource = resourceRepository.save(resource);
                log.info("Recurso '{}' classificado como API — estava sem tipo.", resourceName);
            }
        }
        return resource;
    }

    private String descriptionOf(Method method, String resourceName) {
        ArchbaseResource daClasse = method.getDeclaringClass().getAnnotation(ArchbaseResource.class);
        if (daClasse != null && StringUtils.isNotEmpty(daClasse.description())) {
            return daClasse.description();
        }
        return resourceName;
    }

    /**
     * @return a capacidade no catálogo, ou {@code null} quando nada foi gravado — em modo relatório,
     *         a ação nova ainda não existe, e não há linha à qual pendurar dependência
     */
    /**
     * @return a capacidade no catálogo, ou {@code null} quando nada foi gravado — em modo relatório,
     *         a ação nova ainda não existe, e não há linha à qual pendurar dependência
     */
    private ActionEntity synchronizeAction(String actionName, ResourceEntity resource,
                                           MetadadosDaCapacidade declarado) {
        ActionEntity action;
        Optional<ActionEntity> actionEntityOptional =
                actionRepository.findByActionNameAndResourceName(actionName, resource.getName());

        if (actionEntityOptional.isEmpty()) {
            if (somenteRelatorio()) {
                log.warn("[report] criaria a ação '{}:{}'{}", resource.getName(), actionName,
                        declarado.minimumLevel() == null
                                ? "" : " com nível mínimo " + declarado.minimumLevel());
                return null;
            }
            action = new ActionEntity();
            action.setName(actionName);
            action.setResource(resource);
            action.setDescription(declarado.description());
            action.setLabel(declarado.label());
            action.setCategory(declarado.category());
            action.setActive(true);
            // Semente: a partir daqui quem manda é o admin, igual já acontece com a descrição.
            action.setMinimumLevel(declarado.minimumLevel());
            action.setCreateEntityDate(LocalDateTime.now());
            action.setCreatedByUser("archbase");
            return actionRepository.save(action);
        }

        action = actionEntityOptional.get();
        boolean mudou = false;

        if (!action.getActive()) {
            if (somenteRelatorio()) {
                log.warn("[report] reativaria a ação '{}:{}'", resource.getName(), actionName);
                return action;
            }
            action.setActive(true);
            mudou = true;
        }

        if (somenteRelatorio()) {
            return action;
        }

        if (ressemeando()) {
            mudou |= reescrever(action, declarado, resource.getName(), actionName);
        } else {
            // SEMEIA O QUE NUNCA FOI SEMEADO, e só isso.
            //
            // Rótulo e categoria são campos novos: toda capacidade existente os tem nulos, e nulo
            // aqui não é escolha do admin — é ausência do campo na versão em que a linha nasceu.
            // Preenchê-los quando o código os declara é a mesma classificação retroativa que o tipo
            // do recurso recebeu, e pela mesma razão: sem ela, a separação nasceria vazia justamente
            // nas bases que já têm catálogo.
            //
            // A descrição fica de fora: ela sempre existiu, nunca é nula, e sobrescrevê-la é
            // exatamente o que o modo refresh existe para fazer sob decisão explícita.
            if (action.getLabel() == null && declarado.label() != null) {
                action.setLabel(declarado.label());
                mudou = true;
            }
            if (action.getCategory() == null && declarado.category() != null) {
                action.setCategory(declarado.category());
                mudou = true;
            }
        }

        if (mudou) {
            action.setUpdateEntityDate(LocalDateTime.now());
            action.setLastModifiedByUser("archbase");
            action = actionRepository.save(action);
        }
        return action;
    }

    /**
     * Reescreve os textos da capacidade a partir do código, registrando o que trocou.
     *
     * <p>Cada substituição vai para o log com os dois valores. Um modo que descarta trabalho do
     * admin em silêncio seria indistinguível de perda de dado.
     */
    private boolean reescrever(ActionEntity action, MetadadosDaCapacidade declarado,
                               String resourceName, String actionName) {
        boolean mudou = false;

        if (declarado.description() != null && !declarado.description().equals(action.getDescription())) {
            log.warn("[refresh] '{}:{}' descrição: \"{}\" -> \"{}\"",
                    resourceName, actionName, action.getDescription(), declarado.description());
            action.setDescription(declarado.description());
            mudou = true;
        }

        // Rótulo e categoria seguem o que o código diz, INCLUSIVE quando ele deixou de declarar:
        // remover o atributo da anotação e ver o valor antigo permanecer seria o modo mentindo
        // sobre o que faz.
        if (!java.util.Objects.equals(declarado.label(), action.getLabel())) {
            log.warn("[refresh] '{}:{}' rótulo: \"{}\" -> \"{}\"",
                    resourceName, actionName, action.getLabel(), declarado.label());
            action.setLabel(declarado.label());
            mudou = true;
        }

        if (!java.util.Objects.equals(declarado.category(), action.getCategory())) {
            log.warn("[refresh] '{}:{}' categoria: \"{}\" -> \"{}\"",
                    resourceName, actionName, action.getCategory(), declarado.category());
            action.setCategory(declarado.category());
            mudou = true;
        }

        return mudou;
    }

    private void disableUnusedActionsAndResources() {
        QActionEntity qAction = QActionEntity.actionEntity;
        BooleanExpression actionPredicate = qAction.resource.type.eq(TipoRecurso.API);
        List<ActionEntity> allAPIActions = actionRepository.findAll(actionPredicate);

        QResourceEntity qResource = QResourceEntity.resourceEntity;
        BooleanExpression resourcePredicate = qResource.type.eq(TipoRecurso.API);
        List<ResourceEntity> allAPIResources = resourceRepository.findAll(resourcePredicate);

        // Uma varredura só do que o código declara agora — evita repetir getMethodsAnnotatedWith
        // uma vez por linha do catálogo, como acontecia antes.
        Set<Method> declarados = reflections.getMethodsAnnotatedWith(HasPermission.class);

        List<ActionEntity> acoesADesativar = new ArrayList<>();
        allAPIActions.forEach(action -> {
            if (action.getActive() != null && action.getActive() && !actionStillExists(action, declarados)) {
                acoesADesativar.add(action);
            }
        });

        List<ResourceEntity> recursosADesativar = new ArrayList<>();
        allAPIResources.forEach(resource -> {
            if (resource.getActive() != null && resource.getActive() && !resourceStillExists(resource, declarados)) {
                recursosADesativar.add(resource);
            }
        });

        if (acoesADesativar.isEmpty() && recursosADesativar.isEmpty()) {
            return;
        }

        if (somenteRelatorio()) {
            // O aviso que faltava. Desativar em silêncio é o que transformou uma varredura correta
            // num incidente: o catálogo some da tela e ninguém liga o fato à subida da aplicação.
            log.warn("[report] DESATIVARIA {} ação(ões) e {} recurso(s) de tipo API por não haver "
                            + "@HasPermission correspondente. Ações: {}. Recursos: {}.",
                    acoesADesativar.size(), recursosADesativar.size(),
                    acoesADesativar.stream()
                            .map(a -> a.getResource().getName() + ":" + a.getName()).toList(),
                    recursosADesativar.stream().map(ResourceEntity::getName).toList());
            return;
        }

        log.warn("Sincronização vai desativar {} ação(ões) e {} recurso(s) de tipo API sem "
                        + "@HasPermission correspondente. Rode com archbase.security.sync.mode=report "
                        + "para ver a lista antes de aplicar.",
                acoesADesativar.size(), recursosADesativar.size());

        acoesADesativar.forEach(action -> {
            action.setActive(false);
            action.setUpdateEntityDate(LocalDateTime.now());
            action.setLastModifiedByUser("archbase");
            actionRepository.save(action);
        });

        recursosADesativar.forEach(resource -> {
            resource.setActive(false);
            resource.setUpdateEntityDate(LocalDateTime.now());
            resource.setLastModifiedByUser("archbase");
            resourceRepository.save(resource);
        });
    }

    private boolean actionStillExists(ActionEntity action, Set<Method> declarados) {
        return declarados.stream().anyMatch(method -> {
            HasPermission permission = method.getAnnotation(HasPermission.class);
            String resourceName = resolveResourceName(method, permission);
            return permission.action().equals(action.getName())
                    && action.getResource().getName().equals(resourceName);
        });
    }

    private boolean resourceStillExists(ResourceEntity resource, Set<Method> declarados) {
        return declarados.stream().anyMatch(method -> {
            HasPermission permission = method.getAnnotation(HasPermission.class);
            return resource.getName().equals(resolveResourceName(method, permission));
        });
    }
}
