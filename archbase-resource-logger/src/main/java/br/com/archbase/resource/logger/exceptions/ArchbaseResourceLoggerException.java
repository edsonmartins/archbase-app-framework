package br.com.archbase.resource.logger.exceptions;

public class ArchbaseResourceLoggerException extends RuntimeException {

    public ArchbaseResourceLoggerException() {
    }

    public ArchbaseResourceLoggerException(String message) {
        super(message);
    }

    public ArchbaseResourceLoggerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchbaseResourceLoggerException(Throwable cause) {
        super(cause);
    }

    public ArchbaseResourceLoggerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
