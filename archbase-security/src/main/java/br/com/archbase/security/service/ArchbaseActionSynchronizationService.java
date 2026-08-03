package br.com.archbase.security.service;
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

    @PostConstruct
    public void initialize() {
        if (StringUtils.isEmpty(scanPackages)) {
            log.warn("Nenhum pacote de varredura especificado para segurança do Archbase. Defina a propriedade 'archbase.security.scan-packages'.");
            return;
        }
        this.reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(scanPackages.split(","))
                .setScanners(Scanners.MethodsAnnotated, Scanners.TypesAnnotated));
        try {
            synchronizeActionsAndResources();
        } catch (Exception ex) {
            log.error("Não foi possível sincronizar as ações do sistema {}",ex.getMessage());
        }
    }

    /** {@code true} quando a varredura não deve escrever nada. */
    private boolean somenteRelatorio() {
        return MODE_REPORT.equalsIgnoreCase(syncMode);
    }

    protected void synchronizeActionsAndResources() {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(HasPermission.class);

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
                log.error("@HasPermission em {}#{} não declara resource, e a classe não tem "
                                + "@ArchbaseResource. A capacidade não pôde ser registrada.",
                        method.getDeclaringClass().getName(), method.getName());
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
        return AuthorizationAnnotationUtils.resolveResourceName(method, permission.resource());
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
