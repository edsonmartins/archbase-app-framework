package br.com.archbase.security.service;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.QActionEntity;
import br.com.archbase.security.persistence.QResourceEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.List;

@Service
@Slf4j
public class ArchbaseActionSynchronizationService {

    private final ActionJpaRepository actionRepository; // Repositório de ações
    private final ResourceJpaRepository resourceRepository; // Repositório de recursos

    private Reflections reflections;

    @Value("${archbase.security.scan-packages:}")
    private String scanPackages;


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
                .setScanners(Scanners.MethodsAnnotated));
        try {
            synchronizeActionsAndResources();
        } catch (Exception ex) {
            log.error("Não foi possível sincronizar as ações do sistema {}",ex.getMessage());
        }
    }

    protected void synchronizeActionsAndResources() {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(HasPermission.class);
        for (Method method : methods) {
            HasPermission permission = method.getAnnotation(HasPermission.class);
            String actionName = permission.action();
            String description = permission.description();
            String resourceName = permission.resource();

            ResourceEntity resource = ensureResourceExists(resourceName);
            synchronizeAction(actionName, description, resource);
        }
        disableUnusedActionsAndResources();
    }

    private ResourceEntity ensureResourceExists(String resourceName) {
        ResourceEntity resource = resourceRepository.findByName(resourceName);
        if (resource == null) {
            resource = new ResourceEntity();
            resource.setName(resourceName);
            resource.setDescription(resourceName);
            resource.setCreateEntityDate(LocalDateTime.now());
            resource.setCreatedByUser("archbase");
            resource.setActive(true);
            resource.setType(TipoRecurso.API);
            resourceRepository.save(resource);
        } else {
            if (!resource.getActive()) {
                resource.setUpdateEntityDate(LocalDateTime.now());
                resource.setActive(true);
                resource.setLastModifiedByUser("archbase");
                resource.setType(TipoRecurso.API);
                resource = resourceRepository.save(resource);
            }
        }
        return resource;
    }

    private void synchronizeAction(String actionName, String description, ResourceEntity resource) {
        ActionEntity action = null;
        Optional<ActionEntity> actionEntityOptional = actionRepository.findByActionNameAndResourceName(actionName, resource.getName());
        if (actionEntityOptional.isEmpty()) {
            action = new ActionEntity();
            action.setName(actionName);
            action.setResource(resource);
            action.setDescription(description);
            action.setActive(true);
            action.setCreateEntityDate(LocalDateTime.now());
            action.setCreatedByUser("archbase");
            actionRepository.save(action);
        } else {
            action = actionEntityOptional.get();
            if (!action.getActive()) {
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

        // Desativar ações e recursos não encontrados
        allAPIActions.forEach(action -> {
            if (!actionStillExists(action)) {
                action.setActive(false);
                action.setUpdateEntityDate(LocalDateTime.now());
                action.setLastModifiedByUser("archbase");
                actionRepository.save(action);
            }
        });

        allAPIResources.forEach(resource -> {
            if (!resourceStillExists(resource)) {
                resource.setActive(false);
                resource.setUpdateEntityDate(LocalDateTime.now());
                resource.setLastModifiedByUser("archbase");
                resourceRepository.save(resource);
            }
        });
    }

    private boolean actionStillExists(ActionEntity action) {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(HasPermission.class);
        return methods.stream().anyMatch(method -> {
            HasPermission permission = method.getAnnotation(HasPermission.class);
            return permission.action().equals(action.getName()) &&
                    permission.resource().equals(action.getResource().getName());
        });
    }

    private boolean resourceStillExists(ResourceEntity resource) {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(HasPermission.class);
        return methods.stream().anyMatch(method -> {
            HasPermission permission = method.getAnnotation(HasPermission.class);
            return permission.resource().equals(resource.getName());
        });
    }
}
