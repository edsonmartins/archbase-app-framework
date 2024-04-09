package br.com.archbase.ddd.infraestructure.exceptions;

import br.com.archbase.error.handling.ArchbaseRuntimeException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

public class ArchbaseServiceException extends ArchbaseRuntimeException {

    public ArchbaseServiceException(MessageSource messageSource, String code, String message) {
        super(messageSource, code, message);
    }

    public ArchbaseServiceException(MessageSource messageSource, HttpStatus statusCode, String code, String message) {
        super(messageSource, statusCode, code, message);
    }

    public ArchbaseServiceException(MessageSource messageSource, String code, Object... values) {
        super(messageSource, code, values);
    }

    public ArchbaseServiceException(MessageSource messageSource, HttpStatus statusCode, String code, Object... values) {
        super(messageSource, statusCode, code, values);
    }
}
