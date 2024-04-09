package br.com.archbase.error.handling;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.util.Locale;

/**
 * @author edsonmartins
 */
public class ArchbaseRuntimeException extends RuntimeException {

    protected final HttpStatus statusCode;
    protected final String code;
    protected final String message;
    protected final transient MessageSource messageSource;


    public ArchbaseRuntimeException(MessageSource messageSource, String code, String message) {
        super(code);
        this.code = code;
        this.message = message;
        this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.messageSource = messageSource;
    }

    public ArchbaseRuntimeException(MessageSource messageSource, HttpStatus statusCode, String code, String message) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
        this.messageSource = messageSource;
    }

    public ArchbaseRuntimeException(MessageSource messageSource, String code, Object... values) {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        this.code = code;
        this.message = messageSource.getMessage(code, values, Locale.getDefault());
        this.statusCode = null;
        this.messageSource = messageSource;
    }

    public ArchbaseRuntimeException(MessageSource messageSource, HttpStatus statusCode, String code, Object... values) {
        this.messageSource = messageSource;
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        this.code = code;
        this.message = messageSource.getMessage(code, values, Locale.getDefault());
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
