package br.com.archbase.modulith.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementação padrão do registro de módulos.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class DefaultModuleRegistry implements ModuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultModuleRegistry.class);

    private final Map<String, ModuleDescriptor> modules = new ConcurrentHashMap<>();

    @Override
    public void register(ModuleDescriptor module) {
        Objects.requireNonNull(module, "Module descriptor cannot be null");
        Objects.requireNonNull(module.getName(), "Module name cannot be null");

        if (modules.containsKey(module.getName())) {
            throw new ModuleRegistrationException(module.getName(),
                    "Module '" + module.getName() + "' is already registered");
        }

        modules.put(module.getName(), module);
        log.info("Registered module: {} v{}", module.getName(), module.getVersion());
    }

    @Override
    public boolean unregister(String moduleName) {
        ModuleDescriptor removed = modules.remove(moduleName);
        if (removed != null) {
            log.info("Unregistered module: {}", moduleName);
            return true;
        }
        return false;
    }

    @Override
    public Optional<ModuleDescriptor> getModule(String name) {
        return Optional.ofNullable(modules.get(name));
    }

    @Override
    public List<ModuleDescriptor> getAllModules() {
        return List.copyOf(modules.values());
    }

    @Override
    public List<ModuleDescriptor> getEnabledModules() {
        return modules.values().stream()
                .filter(ModuleDescriptor::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModuleDescriptor> getDependencies(String moduleName) {
        return getModule(moduleName)
                .map(module -> module.getDependencies().stream()
                        .map(modules::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    public List<ModuleDescriptor> getDependents(String moduleName) {
        return modules.values().stream()
                .filter(m -> m.getDependencies().contains(moduleName))
                .collect(Collectors.toList());
    }

    @Override
    public void validateDependencies() {
        List<String> errors = new ArrayList<>();

        for (ModuleDescriptor module : modules.values()) {
            // Verificar dependências ausentes
            List<String> missing = module.getDependencies().stream()
                    .filter(dep -> !modules.containsKey(dep))
                    .collect(Collectors.toList());

            if (!missing.isEmpty()) {
                errors.add(String.format("Module '%s' has missing dependencies: %s",
                        module.getName(), missing));
            }
        }

        // Verificar ciclos de dependência
        List<String> cycles = detectCycles();
        if (!cycles.isEmpty()) {
            errors.add("Cyclic dependencies detected: " + cycles);
        }

        if (!errors.isEmpty()) {
            throw new ModuleDependencyException("*",
                    "Dependency validation failed:\n" + String.join("\n", errors));
        }

        log.info("Module dependencies validated successfully");
    }

    @Override
    public ModuleHealth getHealth(String moduleName) {
        return getModule(moduleName)
                .map(module -> {
                    if (module.getState() != ModuleState.STARTED) {
                        return ModuleHealth.builder()
                                .moduleName(moduleName)
                                .state(module.getState())
                                .healthy(false)
                                .message("Module is not started")
                                .build();
                    }

                    boolean healthy = true;
                    String message = "Module is healthy";

                    if (module.hasLifecycle()) {
                        try {
                            healthy = module.getLifecycle().isHealthy();
                            if (!healthy) {
                                message = "Module reports unhealthy state";
                            }
                        } catch (Exception e) {
                            healthy = false;
                            message = "Health check failed: " + e.getMessage();
                        }
                    }

                    return ModuleHealth.builder()
                            .moduleName(moduleName)
                            .state(module.getState())
                            .healthy(healthy)
                            .message(message)
                            .build();
                })
                .orElse(ModuleHealth.notFound(moduleName));
    }

    @Override
    public List<ModuleHealth> getAllHealth() {
        return modules.keySet().stream()
                .map(this::getHealth)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isRegistered(String moduleName) {
        return modules.containsKey(moduleName);
    }

    @Override
    public List<ModuleDescriptor> getStartupOrder() {
        return topologicalSort(getEnabledModules());
    }

    @Override
    public List<ModuleDescriptor> getShutdownOrder() {
        List<ModuleDescriptor> startupOrder = getStartupOrder();
        Collections.reverse(startupOrder);
        return startupOrder;
    }

    /**
     * Detecta ciclos de dependência usando DFS.
     */
    private List<String> detectCycles() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> cycles = new ArrayList<>();

        for (String moduleName : modules.keySet()) {
            if (detectCyclesDFS(moduleName, visited, recursionStack, cycles)) {
                break; // Encontrou ciclo
            }
        }

        return cycles;
    }

    private boolean detectCyclesDFS(String current, Set<String> visited,
                                    Set<String> recursionStack, List<String> cycles) {
        if (recursionStack.contains(current)) {
            cycles.add(current);
            return true;
        }

        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);
        recursionStack.add(current);

        ModuleDescriptor module = modules.get(current);
        if (module != null) {
            for (String dependency : module.getDependencies()) {
                if (detectCyclesDFS(dependency, visited, recursionStack, cycles)) {
                    cycles.add(current);
                    return true;
                }
            }
        }

        recursionStack.remove(current);
        return false;
    }

    /**
     * Ordena os módulos topologicamente considerando dependências.
     */
    private List<ModuleDescriptor> topologicalSort(List<ModuleDescriptor> modules) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>();

        // Inicializar
        for (ModuleDescriptor module : modules) {
            inDegree.put(module.getName(), 0);
            adjList.put(module.getName(), new ArrayList<>());
        }

        // Construir grafo
        for (ModuleDescriptor module : modules) {
            for (String dep : module.getDependencies()) {
                if (adjList.containsKey(dep)) {
                    adjList.get(dep).add(module.getName());
                    inDegree.merge(module.getName(), 1, Integer::sum);
                }
            }
        }

        // Ordenar por ordem configurada e então por nome
        PriorityQueue<ModuleDescriptor> queue = new PriorityQueue<>(
                Comparator.comparingInt(ModuleDescriptor::getOrder)
                        .thenComparing(ModuleDescriptor::getName));

        for (ModuleDescriptor module : modules) {
            if (inDegree.get(module.getName()) == 0) {
                queue.offer(module);
            }
        }

        List<ModuleDescriptor> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            ModuleDescriptor current = queue.poll();
            result.add(current);

            for (String dependent : adjList.get(current.getName())) {
                int newDegree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newDegree);
                if (newDegree == 0) {
                    getModule(dependent).ifPresent(queue::offer);
                }
            }
        }

        return result;
    }
}
