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


    public ArchbaseActionSynchronizationService(ActionJpaRepository actionRepository, ResourceJpaRepository resourceJpaRepository) {
        this.actionRepository = actionRepository;
        this.resourceRepository = resourceJpaRepository;
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

    protected void synchronizeActionsAndResources() {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(HasPermission.class);
        boolean houveMetodoSemRecurso = false;

        if (somenteRelatorio()) {
            log.warn("archbase.security.sync.mode=report — a sincronização NÃO vai escrever nada. "
                    + "{} método(s) anotado(s) com @HasPermission encontrado(s).", methods.size());
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

            synchronizeAction(actionName, description, resource, minimumLevelOf(permission));
        }

        if (houveMetodoSemRecurso) {
            // Desativar exige a lista COMPLETA do que o código declara. Com pelo menos um método
            // sem recurso resolvível, essa lista está incompleta — e desativar a partir dela
            // derrubaria do catálogo capacidades que existem, só não foram reconhecidas. O
            // resultado seria 403 permanente num endpoint que o admin nem consegue mais conceder.
            log.error("Desativação de capacidades NÃO executada: há método(s) com @HasPermission "
                    + "cujo recurso não pôde ser resolvido. Corrija os avisos acima primeiro — "
                    + "desativar com a lista incompleta removeria capacidades válidas do catálogo.");
            return;
        }

        disableUnusedActionsAndResources();
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

    private void synchronizeAction(String actionName, String description, ResourceEntity resource,
                                   AccessLevel minimumLevel) {
        ActionEntity action = null;
        Optional<ActionEntity> actionEntityOptional = actionRepository.findByActionNameAndResourceName(actionName, resource.getName());
        if (actionEntityOptional.isEmpty()) {
            if (somenteRelatorio()) {
                log.warn("[report] criaria a ação '{}:{}'{}", resource.getName(), actionName,
                        minimumLevel == null ? "" : " com nível mínimo " + minimumLevel);
                return;
            }
            action = new ActionEntity();
            action.setName(actionName);
            action.setResource(resource);
            action.setDescription(description);
            action.setActive(true);
            // Semente: a partir daqui quem manda é o admin, igual já acontece com a descrição.
            action.setMinimumLevel(minimumLevel);
            action.setCreateEntityDate(LocalDateTime.now());
            action.setCreatedByUser("archbase");
            actionRepository.save(action);
        } else {
            action = actionEntityOptional.get();
            if (!action.getActive()) {
                if (somenteRelatorio()) {
                    log.warn("[report] reativaria a ação '{}:{}'", resource.getName(), actionName);
                    return;
                }
                action.setActive(true);
                action.setUpdateEntityDate(LocalDateTime.now());
                action.setLastModifiedByUser("archbase");
                actionRepository.save(action);
            }
        }
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
