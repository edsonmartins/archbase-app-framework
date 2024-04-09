package br.com.archbase.plugin.manager;

public class PluginLoaderException extends RuntimeException {
    public PluginLoaderException() {
    }

    public PluginLoaderException(String message) {
        super(message);
    }

    public PluginLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginLoaderException(Throwable cause) {
        super(cause);
    }

    public PluginLoaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
