package br.com.archbase.error.handling.handler;


import br.com.archbase.error.handling.ArchbaseApiErrorResponse;
import br.com.archbase.error.handling.ArchbaseErrorHandlingProperties;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * @author edsonmartins
 */
public class ObjectOptimisticLockingFailureArchbaseApiExceptionHandler extends AbstractArchbaseApiExceptionHandler {

    public ObjectOptimisticLockingFailureArchbaseApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        super(properties);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ObjectOptimisticLockingFailureException;
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {
        ArchbaseApiErrorResponse response = new ArchbaseApiErrorResponse(HttpStatus.CONFLICT,
                getErrorCode(exception),
                exception.getMessage());
        ObjectOptimisticLockingFailureException ex = (ObjectOptimisticLockingFailureException) exception;
        response.addErrorProperty("identifier", ex.getIdentifier());
        response.addErrorProperty("persistentClassName", ex.getPersistentClassName());
        return response;
    }
}
