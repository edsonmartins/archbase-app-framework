package br.com.archbase.spring.boot.configuration;

public class ArchbaseConfigurationException extends RuntimeException {
    public ArchbaseConfigurationException() {
    }

    public ArchbaseConfigurationException(String message) {
        super(message);
    }

    public ArchbaseConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchbaseConfigurationException(Throwable cause) {
        super(cause);
    }

    public ArchbaseConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
