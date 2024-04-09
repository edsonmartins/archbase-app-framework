package br.com.archbase.plugin.manager.update;

public class PluginUpdateException extends RuntimeException {
    public PluginUpdateException() {
    }

    public PluginUpdateException(String message) {
        super(message);
    }

    public PluginUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginUpdateException(Throwable cause) {
        super(cause);
    }

    public PluginUpdateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
