package br.com.archbase.error.handling;


import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Locale;

/**
 * @author edsonmartins
 */
@ControllerAdvice(annotations = RestController.class)
public class ErrorHandlingControllerAdvice {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ErrorHandlingControllerAdvice.class);

    private final ArchbaseErrorHandlingProperties properties;
    private final List<ArchbaseApiExceptionHandler> handlers;
    private final FallbackApiExceptionHandler fallbackHandler;

    public ErrorHandlingControllerAdvice(ArchbaseErrorHandlingProperties properties,
                                         List<ArchbaseApiExceptionHandler> handlers,
                                         FallbackApiExceptionHandler fallbackHandler) {
        this.properties = properties;
        this.handlers = handlers;
        this.fallbackHandler = fallbackHandler;
        this.handlers.sort(AnnotationAwareOrderComparator.INSTANCE);

        log.info("Tratamento de erros Spring Boot Starter ativo com {} manipuladores", this.handlers.size());
        log.debug("Manipuladores: {}", this.handlers);
    }

    @ExceptionHandler
    public <T> ResponseEntity<T> handleException(Throwable exception, WebRequest webRequest, Locale locale) {
        log.debug("webRequest: {}", webRequest);
        log.debug("locale: {}", locale);
        logException(exception);

        ArchbaseApiErrorResponse errorResponse = null;
        for (ArchbaseApiExceptionHandler handler : handlers) {
            if (handler.canHandle(exception)) {
                errorResponse = handler.handle(exception);
                break;
            }
        }

        if (errorResponse == null) {
            errorResponse = fallbackHandler.handle(exception);
        }

        return (ResponseEntity<T>) ResponseEntity.status(errorResponse.getHttpStatus())
                .body(errorResponse);
    }

    private void logException(Throwable exception) {
        if (properties.getExceptionLogging() == ArchbaseErrorHandlingProperties.ExceptionLogging.WITH_STACKTRACE) {
            log.error(exception.getMessage(), exception);
        } else if (properties.getExceptionLogging() == ArchbaseErrorHandlingProperties.ExceptionLogging.MESSAGE_ONLY) {
            log.error(exception.getMessage());
        }
    }
}
