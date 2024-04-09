package br.com.archbase.error.handling.handler;

import br.com.archbase.error.handling.ArchbaseApiExceptionHandler;
import br.com.archbase.error.handling.ArchbaseErrorHandlingProperties;

/**
 * @author edsonmartins
 */
public abstract class AbstractArchbaseApiExceptionHandler implements ArchbaseApiExceptionHandler {
    protected final ArchbaseErrorHandlingProperties properties;

    protected AbstractArchbaseApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        this.properties = properties;
    }

    protected String getErrorCode(Throwable exception) {
        return replaceCodeWithConfiguredOverrideIfPresent(exception.getClass().getName());
    }

    protected String replaceCodeWithConfiguredOverrideIfPresent(String code) {
        return properties.getCodes().getOrDefault(code, code);
    }

    protected boolean hasConfiguredOverrideForCode(String code) {
        return properties.getCodes().containsKey(code);
    }

    protected boolean hasConfiguredOverrideForMessage(String key) {
        return properties.getMessages().containsKey(key);
    }

    protected String getOverrideMessage(String key) {
        return properties.getMessages().get(key);
    }
}
