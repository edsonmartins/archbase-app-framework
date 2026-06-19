package br.com.archbase.modulith.communication;

import br.com.archbase.modulith.core.ModuleException;

/**
 * Exceção lançada quando há erro na execução de uma requisição de módulo.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class ModuleRequestException extends ModuleException {

    private final String operationName;

    public ModuleRequestException(String moduleName, String operationName, String message) {
        super(moduleName, message);
        this.operationName = operationName;
    }

    public ModuleRequestException(String moduleName, String operationName, String message, Throwable cause) {
        super(moduleName, message, cause);
        this.operationName = operationName;
    }

    public String getOperationName() {
        return operationName;
    }
}
