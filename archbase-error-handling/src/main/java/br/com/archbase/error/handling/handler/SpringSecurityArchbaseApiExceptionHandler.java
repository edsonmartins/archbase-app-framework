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

    /**
     * Aceita as classes mapeadas e as subclasses delas.
     *
     * <p>Até a 3.5.0 a comparação era pela classe exata. A negação de {@code @HasPermission} — e de qualquer
     * segurança de método do Spring Security 7 — lança {@code AuthorizationDeniedException}, subclasse de
     * {@link AccessDeniedException}, e caía no tratamento genérico: 500 em vez de 403.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return classeMapeada(exception) != null;
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {
        Class<?> mapeada = classeMapeada(exception);
        HttpStatus httpStatus = mapeada == null ? INTERNAL_SERVER_ERROR : EXCEPTION_TO_STATUS_MAPPING.get(mapeada);
        return new ArchbaseApiErrorResponse(httpStatus,
                codigo(exception, mapeada),
                exception.getMessage());
    }

    /**
     * O código configurado para a própria classe, se houver; senão, o da classe mapeada (uma
     * {@code AuthorizationDeniedException} sai como {@code ACCESS_DENIED}, como a {@code AccessDeniedException}).
     */
    private String codigo(Throwable exception, Class<?> mapeada) {
        String propria = exception.getClass().getName();
        if (mapeada == null || mapeada == exception.getClass() || hasConfiguredOverrideForCode(propria)) {
            return getErrorCode(exception);
        }
        return replaceCodeWithConfiguredOverrideIfPresent(mapeada.getName());
    }

    /** A classe mapeada mais próxima na hierarquia da exceção, ou {@code null}. */
    private static Class<?> classeMapeada(Throwable exception) {
        for (Class<?> tipo = exception.getClass(); tipo != null && tipo != Throwable.class; tipo = tipo.getSuperclass()) {
            if (EXCEPTION_TO_STATUS_MAPPING.containsKey(tipo)) {
                return tipo;
            }
        }
        return null;
    }
}
