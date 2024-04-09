package br.com.archbase.plugin.manager.spring;

public class SpringExtensionFactoryException extends RuntimeException {
    public SpringExtensionFactoryException() {
    }

    public SpringExtensionFactoryException(String message) {
        super(message);
    }

    public SpringExtensionFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpringExtensionFactoryException(Throwable cause) {
        super(cause);
    }

    public SpringExtensionFactoryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
