package br.com.archbase.modulith.core;

import java.util.List;

/**
 * Exceção lançada quando há problemas nas dependências de módulos.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class ModuleDependencyException extends ModuleException {

    private final List<String> missingDependencies;
    private final List<String> cyclicDependencies;

    public ModuleDependencyException(String moduleName, String message) {
        super(moduleName, message);
        this.missingDependencies = List.of();
        this.cyclicDependencies = List.of();
    }

    public ModuleDependencyException(String moduleName, String message,
                                     List<String> missingDependencies) {
        super(moduleName, message);
        this.missingDependencies = missingDependencies;
        this.cyclicDependencies = List.of();
    }

    public ModuleDependencyException(String moduleName, String message,
                                     List<String> missingDependencies,
                                     List<String> cyclicDependencies) {
        super(moduleName, message);
        this.missingDependencies = missingDependencies;
        this.cyclicDependencies = cyclicDependencies;
    }

    public List<String> getMissingDependencies() {
        return missingDependencies;
    }

    public List<String> getCyclicDependencies() {
        return cyclicDependencies;
    }

    public boolean hasMissingDependencies() {
        return !missingDependencies.isEmpty();
    }

    public boolean hasCyclicDependencies() {
        return !cyclicDependencies.isEmpty();
    }
}
