package br.com.archbase.modulith.spring;

import br.com.archbase.modulith.annotations.Module;
import br.com.archbase.modulith.communication.IntegrationEventBus;
import br.com.archbase.modulith.communication.ModuleGateway;
import br.com.archbase.modulith.communication.SimpleIntegrationEventBus;
import br.com.archbase.modulith.communication.SimpleModuleGateway;
import br.com.archbase.modulith.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Auto-configuração do Modular Monolith para Spring Boot.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ModulithProperties.class)
@ConditionalOnProperty(prefix = "archbase.modulith", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ModuleAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ModuleAutoConfiguration.class);

    @Autowired
    private ModulithProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public ModuleRegistry moduleRegistry() {
        return new DefaultModuleRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationEventBus integrationEventBus(ObjectMapper objectMapper) {
        ExecutorService executor = Executors.newFixedThreadPool(
                properties.getEventBus().getThreadPoolSize());
        return new SimpleIntegrationEventBus("main", executor, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuleGateway moduleGateway(ModuleRegistry moduleRegistry) {
        ExecutorService executor = Executors.newFixedThreadPool(
                properties.getGateway().getThreadPoolSize());
        return new SimpleModuleGateway(moduleRegistry, executor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper modulithObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules();
    }

    @Bean
    public ModuleDiscoveryListener moduleDiscoveryListener(
            ApplicationContext applicationContext,
            ModuleRegistry moduleRegistry,
            IntegrationEventBus integrationEventBus,
            ModuleGateway moduleGateway) {
        return new ModuleDiscoveryListener(
                applicationContext,
                moduleRegistry,
                integrationEventBus,
                moduleGateway,
                properties);
    }

    /**
     * Listener que descobre e registra módulos após a inicialização do contexto.
     */
    public static class ModuleDiscoveryListener {

        private final ApplicationContext applicationContext;
        private final ModuleRegistry moduleRegistry;
        private final IntegrationEventBus integrationEventBus;
        private final ModuleGateway moduleGateway;
        private final ModulithProperties properties;

        public ModuleDiscoveryListener(
                ApplicationContext applicationContext,
                ModuleRegistry moduleRegistry,
                IntegrationEventBus integrationEventBus,
                ModuleGateway moduleGateway,
                ModulithProperties properties) {
            this.applicationContext = applicationContext;
            this.moduleRegistry = moduleRegistry;
            this.integrationEventBus = integrationEventBus;
            this.moduleGateway = moduleGateway;
            this.properties = properties;
        }

        @EventListener
        public void onApplicationEvent(ContextRefreshedEvent event) {
            if (event.getApplicationContext() != applicationContext) {
                return;
            }

            log.info("Starting module discovery...");

            // Descobrir módulos anotados com @Module
            Map<String, Object> moduleBeans = applicationContext.getBeansWithAnnotation(Module.class);

            for (Map.Entry<String, Object> entry : moduleBeans.entrySet()) {
                Object moduleInstance = entry.getValue();
                Class<?> moduleClass = moduleInstance.getClass();
                Module moduleAnnotation = moduleClass.getAnnotation(Module.class);

                if (moduleAnnotation == null) {
                    // Pode ser um proxy - tentar obter da classe real
                    moduleAnnotation = moduleClass.getSuperclass().getAnnotation(Module.class);
                    if (moduleAnnotation == null) {
                        continue;
                    }
                }

                ModuleDescriptor descriptor = ModuleDescriptor.builder()
                        .name(moduleAnnotation.name())
                        .version(moduleAnnotation.version())
                        .description(moduleAnnotation.description())
                        .enabled(moduleAnnotation.enabled())
                        .order(moduleAnnotation.order())
                        .dependencies(new HashSet<>(Arrays.asList(moduleAnnotation.dependsOn())))
                        .moduleClass(moduleClass)
                        .moduleInstance(moduleInstance)
                        .basePackage(moduleClass.getPackageName())
                        .build();

                moduleRegistry.register(descriptor);
            }

            log.info("Discovered {} modules", moduleRegistry.getAllModules().size());

            // Validar dependências se configurado
            if (properties.isValidateDependenciesOnStartup()) {
                moduleRegistry.validateDependencies();
            }

            // Iniciar módulos na ordem correta
            startModules();
        }

        private void startModules() {
            for (ModuleDescriptor module : moduleRegistry.getStartupOrder()) {
                if (!module.isEnabled()) {
                    log.info("Skipping disabled module: {}", module.getName());
                    continue;
                }

                try {
                    module.setState(ModuleState.STARTING);
                    log.info("Starting module: {} v{}", module.getName(), module.getVersion());

                    if (module.hasLifecycle()) {
                        ModuleContext context = ModuleContext.builder()
                                .moduleDescriptor(module)
                                .moduleRegistry(moduleRegistry)
                                .integrationEventBus(integrationEventBus)
                                .moduleGateway(moduleGateway)
                                .applicationContext(applicationContext)
                                .build();

                        module.getLifecycle().onStart(context);
                    }

                    module.setState(ModuleState.STARTED);
                    log.info("Module started: {}", module.getName());

                } catch (Exception e) {
                    module.setState(ModuleState.FAILED);
                    log.error("Failed to start module: {}", module.getName(), e);

                    if (module.hasLifecycle()) {
                        module.getLifecycle().onError(e);
                    }

                    throw new ModuleException(module.getName(),
                            "Failed to start module: " + e.getMessage(), e);
                }
            }
        }
    }
}
