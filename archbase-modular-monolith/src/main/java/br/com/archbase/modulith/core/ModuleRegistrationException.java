package br.com.archbase.modulith.core;

/**
 * Exceção lançada quando há erro no registro de um módulo.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class ModuleRegistrationException extends ModuleException {

    public ModuleRegistrationException(String moduleName, String message) {
        super(moduleName, message);
    }

    public ModuleRegistrationException(String moduleName, String message, Throwable cause) {
        super(moduleName, message, cause);
    }
}
