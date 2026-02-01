package br.com.archbase.error.handling.handler;

import br.com.archbase.error.handling.ArchbaseApiErrorResponse;
import br.com.archbase.error.handling.ArchbaseErrorHandlingProperties;
import org.springframework.util.Assert;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * @author edsonmartins
 */
public class TypeMismatchArchbaseApiExceptionHandler extends AbstractArchbaseApiExceptionHandler {
    public TypeMismatchArchbaseApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        super(properties);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof TypeMismatchException;
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {
        Assert.notNull(exception, "Exceção não pode ser nula");
        ArchbaseApiErrorResponse response = new ArchbaseApiErrorResponse(HttpStatus.BAD_REQUEST,
                getErrorCode(exception),
                exception.getMessage());
        TypeMismatchException ex = (TypeMismatchException) exception;
        String name = "";
        Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null) {
            name = requiredType.getName();
        }
        response.addErrorProperty("property", getPropertyName(ex));
        response.addErrorProperty("rejectedValue", ex.getValue());
        response.addErrorProperty("expectedType", name);
        return response;
    }

    private String getPropertyName(TypeMismatchException exception) {
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return ((MethodArgumentTypeMismatchException) exception).getName();
        } else {
            return exception.getPropertyName();
        }
    }
}
