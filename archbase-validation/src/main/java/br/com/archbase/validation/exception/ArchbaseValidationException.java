package br.com.archbase.validation.exception;

import br.com.archbase.validation.fluentvalidator.context.Error;

import java.util.Collection;

public class ArchbaseValidationException extends RuntimeException{

    private Collection<Error> errors;

    public ArchbaseValidationException() {
    }

    public ArchbaseValidationException(Collection<Error> errors) {
        this.errors = errors;
    }

    public ArchbaseValidationException(String message) {
        super(message);
    }

    public ArchbaseValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchbaseValidationException(Throwable cause) {
        super(cause);
    }

    public ArchbaseValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Collection<Error> getErrors() {
        return errors;
    }
}