package br.com.archbase.validation.exception;

import br.com.archbase.ddd.domain.contracts.ValidationError;

import java.util.Collection;

public class ArchbaseValidationException extends RuntimeException{

    private Collection<ValidationError> validationErrors;

    public ArchbaseValidationException() {
    }

    public ArchbaseValidationException(Collection<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
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

    public Collection<ValidationError> getErrors() {
        return validationErrors;
    }
}