package br.com.archbase.error.handling;

/**
 * @author edsonmartins
 */
public interface ArchbaseApiExceptionHandler {
    boolean canHandle(Throwable exception);

    ArchbaseApiErrorResponse handle(Throwable exception);
}
