package br.com.archbase.mapper.exception;

/**
 * Exceção lançada quando ocorre um erro durante o mapeamento de objetos.
 */
public class MappingException extends RuntimeException {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MappingException(Throwable cause) {
        super(cause);
    }
}
