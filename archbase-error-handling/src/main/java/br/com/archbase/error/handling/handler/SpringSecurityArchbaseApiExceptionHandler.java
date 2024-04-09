package br.com.archbase.error.handling.handler;

import br.com.archbase.error.handling.ArchbaseApiErrorResponse;
import br.com.archbase.error.handling.ArchbaseErrorHandlingProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

/**
 * @author edsonmartins
 */
public class SpringSecurityArchbaseApiExceptionHandler extends AbstractArchbaseApiExceptionHandler {

    private static final Map<Class<? extends Exception>, HttpStatus> EXCEPTION_TO_STATUS_MAPPING;

    static {
        EXCEPTION_TO_STATUS_MAPPING = new HashMap<>();
        EXCEPTION_TO_STATUS_MAPPING.put(AccessDeniedException.class, FORBIDDEN);
        EXCEPTION_TO_STATUS_MAPPING.put(AccountExpiredException.class, BAD_REQUEST);
        EXCEPTION_TO_STATUS_MAPPING.put(AuthenticationCredentialsNotFoundException.class, UNAUTHORIZED);
        EXCEPTION_TO_STATUS_MAPPING.put(AuthenticationServiceException.class, INTERNAL_SERVER_ERROR);
        EXCEPTION_TO_STATUS_MAPPING.put(BadCredentialsException.class, BAD_REQUEST);
        EXCEPTION_TO_STATUS_MAPPING.put(UsernameNotFoundException.class, BAD_REQUEST);
        EXCEPTION_TO_STATUS_MAPPING.put(InsufficientAuthenticationException.class, UNAUTHORIZED);
        EXCEPTION_TO_STATUS_MAPPING.put(LockedException.class, BAD_REQUEST);
        EXCEPTION_TO_STATUS_MAPPING.put(DisabledException.class, BAD_REQUEST);
    }

    public SpringSecurityArchbaseApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        super(properties);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return EXCEPTION_TO_STATUS_MAPPING.containsKey(exception.getClass());
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {
        HttpStatus httpStatus = EXCEPTION_TO_STATUS_MAPPING.getOrDefault(exception.getClass(), INTERNAL_SERVER_ERROR);
        return new ArchbaseApiErrorResponse(httpStatus,
                getErrorCode(exception),
                exception.getMessage());
    }
}
