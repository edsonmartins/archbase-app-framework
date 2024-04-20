package br.com.archbase.security.service;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
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
        synchronizeActionsAndResources();
    }

    protected void synchronizeActionsAndResources() {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(HasPermission.class);
        for (Method method : methods) {
            HasPermission permission = method.getAnnotation(HasPermission.class);
            String actionName = permission.action();
            String resourceName = permission.resource();

            ResourceEntity resource = ensureResourceExists(resourceName);
            synchronizeAction(actionName, resource);
        }
        disableUnusedActionsAndResources();
    }

    private ResourceEntity ensureResourceExists(String resourceName) {
        ResourceEntity resource = resourceRepository.findByName(resourceName);
        if (resource == null) {
            resource = new ResourceEntity();
            resource.setName(resourceName);
            // Setar outros atributos necessários aqui
            resourceRepository.save(resource);
        }
        return resource;
    }

    private void synchronizeAction(String actionName, ResourceEntity resource) {
        ActionEntity action = null;
        Optional<ActionEntity> actionEntityOptional = actionRepository.findByActionNameAndResourceName(actionName, resource.getName());
        if (actionEntityOptional.isEmpty()) {
            action = new ActionEntity();
            action.setName(actionName);
            action.setResource(resource);
            action.setActive(true);
        } else {
            action = actionEntityOptional.get();
        }
        actionRepository.save(action);
    }

    private void disableUnusedActionsAndResources() {
        List<ActionEntity> allActions = actionRepository.findAll();
        List<ResourceEntity> allResources = resourceRepository.findAll();

        // Desativar ações e recursos não encontrados
        allActions.forEach(action -> {
            if (!actionStillExists(action)) {
                action.setActive(false);
                actionRepository.save(action);
            }
        });

        allResources.forEach(resource -> {
            if (!resourceStillExists(resource)) {
                resource.setActive(false);
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
