package br.com.archbase.modulith.core;

/**
 * Exceção base para erros relacionados a módulos.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class ModuleException extends RuntimeException {

    private final String moduleName;

    public ModuleException(String moduleName, String message) {
        super(message);
        this.moduleName = moduleName;
    }

    public ModuleException(String moduleName, String message, Throwable cause) {
        super(message, cause);
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }
}
