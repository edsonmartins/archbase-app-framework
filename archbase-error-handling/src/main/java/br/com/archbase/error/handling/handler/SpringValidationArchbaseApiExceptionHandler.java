package br.com.archbase.error.handling.handler;


import br.com.archbase.error.handling.ArchbaseApiErrorResponse;
import br.com.archbase.error.handling.ArchbaseApiFieldError;
import br.com.archbase.error.handling.ArchbaseApiGlobalError;
import br.com.archbase.error.handling.ArchbaseErrorHandlingProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * @author edsonmartins
 */
public class SpringValidationArchbaseApiExceptionHandler extends AbstractArchbaseApiExceptionHandler {

    public SpringValidationArchbaseApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        super(properties);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof MethodArgumentNotValidException
                || exception instanceof HttpMessageNotReadableException;
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {

        ArchbaseApiErrorResponse response;
        if (exception instanceof MethodArgumentNotValidException) {
            response = new ArchbaseApiErrorResponse(HttpStatus.BAD_REQUEST,
                    getErrorCode(exception),
                    getMessage((MethodArgumentNotValidException) exception));
            BindingResult bindingResult = ((MethodArgumentNotValidException) exception).getBindingResult();
            if (bindingResult.hasFieldErrors()) {
                bindingResult.getFieldErrors().stream()
                        .map(fieldError -> new ArchbaseApiFieldError(getCode(fieldError),
                                fieldError.getField(),
                                getMessage(fieldError),
                                fieldError.getRejectedValue()))
                        .forEach(response::addFieldError);
            }

            if (bindingResult.hasGlobalErrors()) {
                bindingResult.getGlobalErrors().stream()
                        .map(globalError -> new ArchbaseApiGlobalError(replaceCodeWithConfiguredOverrideIfPresent(globalError.getCode()),
                                globalError.getDefaultMessage()))
                        .forEach(response::addGlobalError);
            }
        } else if (exception instanceof HttpMessageNotReadableException) {
            response = new ArchbaseApiErrorResponse(HttpStatus.BAD_REQUEST,
                    replaceCodeWithConfiguredOverrideIfPresent(exception.getClass().getName()),
                    exception.getMessage());

        } else {
            throw new IllegalStateException("métodos canHandle() e handle() não estão em sincronia!");
        }

        return response;
    }

    private String getCode(FieldError fieldError) {
        String fieldSpecificCode = fieldError.getField() + "." + fieldError.getCode();
        if (hasConfiguredOverrideForCode(fieldSpecificCode)) {
            return replaceCodeWithConfiguredOverrideIfPresent(fieldSpecificCode);
        }
        return replaceCodeWithConfiguredOverrideIfPresent(fieldError.getCode());
    }

    private String getMessage(FieldError fieldError) {
        String fieldSpecificKey = fieldError.getField() + "." + fieldError.getCode();
        if (hasConfiguredOverrideForMessage(fieldSpecificKey)) {
            return getOverrideMessage(fieldSpecificKey);
        }
        if (hasConfiguredOverrideForMessage(fieldError.getCode())) {
            return getOverrideMessage(fieldError.getCode());
        }
        return fieldError.getDefaultMessage();
    }

    private String getMessage(MethodArgumentNotValidException exception) {
        return "A validação falhou para o objeto='" + exception.getBindingResult().getObjectName() + "'. Contagem de erros: " + exception.getBindingResult().getErrorCount();
    }
}
