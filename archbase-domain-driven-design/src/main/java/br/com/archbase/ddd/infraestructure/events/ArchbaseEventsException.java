package br.com.archbase.ddd.infraestructure.events;

public class ArchbaseEventsException extends RuntimeException {
    public ArchbaseEventsException() {
    }

    public ArchbaseEventsException(String message) {
        super(message);
    }

    public ArchbaseEventsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchbaseEventsException(Throwable cause) {
        super(cause);
    }

    public ArchbaseEventsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
