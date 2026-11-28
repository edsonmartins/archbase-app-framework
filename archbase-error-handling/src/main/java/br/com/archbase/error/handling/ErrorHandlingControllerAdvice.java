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
        logException(exception, errorResponse.getHttpStatus());

        return (ResponseEntity<T>) ResponseEntity.status(errorResponse.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * Erro do servidor (5xx) em ERROR, como sempre; erro do cliente (4xx) em WARN, com a pilha só em DEBUG. Até a
     * 3.5.0 todo erro saía em ERROR — um 400 de validação disparava o mesmo alerta que uma falha de banco.
     */
    private void logException(Throwable exception, org.springframework.http.HttpStatusCode status) {
        var modo = properties.getExceptionLogging();
        if (modo == ArchbaseErrorHandlingProperties.ExceptionLogging.NO_LOGGING) {
            return;
        }
        if (status != null && !status.is5xxServerError()) {
            log.warn("{} {}", status.value(), exception.getMessage());
            log.debug(exception.getMessage(), exception);
            return;
        }
        if (modo == ArchbaseErrorHandlingProperties.ExceptionLogging.WITH_STACKTRACE) {
            log.error(exception.getMessage(), exception);
        } else {
            log.error(exception.getMessage());
        }
    }
}
