package br.com.archbase.validation.message.exception;

public class ArchbaseMessageInterpolatorException extends RuntimeException {

    public ArchbaseMessageInterpolatorException() {
    }

    public ArchbaseMessageInterpolatorException(String message) {
        super(message);
    }

    public ArchbaseMessageInterpolatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchbaseMessageInterpolatorException(Throwable cause) {
        super(cause);
    }

    public ArchbaseMessageInterpolatorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
