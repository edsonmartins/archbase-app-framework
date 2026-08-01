package br.com.archbase.security.exception;

/**
 * Operação recusada por excesso de tentativas.
 *
 * <p>Distinta de {@code BadCredentialsException} de propósito: o cliente precisa saber que o
 * problema não é a credencial e que insistir agora não adianta — daí o 429 e o
 * {@code Retry-After}, em vez de mais um 401 idêntico aos anteriores.
 */
public class ArchbaseTooManyAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public ArchbaseTooManyAttemptsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
