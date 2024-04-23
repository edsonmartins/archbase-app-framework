package br.com.archbase.security.exception;

public class ArchbaseSecurityException extends RuntimeException{
    public ArchbaseSecurityException() {
    }

    public ArchbaseSecurityException(String message) {
        super(message);
    }

    public ArchbaseSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchbaseSecurityException(Throwable cause) {
        super(cause);
    }

    public ArchbaseSecurityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
