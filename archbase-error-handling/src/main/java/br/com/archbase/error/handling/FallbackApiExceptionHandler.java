package br.com.archbase.error.handling;

/**
 * @author edsonmartins
 */
public interface FallbackApiExceptionHandler {
    ArchbaseApiErrorResponse handle(Throwable exception);
}
