package br.com.archbase.modulith.communication;

import java.time.Duration;

/**
 * Exceção lançada quando uma requisição de módulo excede o timeout.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public class ModuleRequestTimeoutException extends ModuleRequestException {

    private final Duration timeout;

    public ModuleRequestTimeoutException(String moduleName, String operationName, Duration timeout) {
        super(moduleName, operationName,
                String.format("Request to module '%s' timed out after %s", moduleName, timeout));
        this.timeout = timeout;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
