package br.com.archbase.modulith.communication;

import br.com.archbase.modulith.core.ModuleException;

/**
 * Exceção lançada quando um módulo não é encontrado.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class ModuleNotFoundException extends ModuleException {

    public ModuleNotFoundException(String moduleName) {
        super(moduleName, "Module not found: " + moduleName);
    }

    public ModuleNotFoundException(String moduleName, String message) {
        super(moduleName, message);
    }
}
