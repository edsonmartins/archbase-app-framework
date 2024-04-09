package br.com.archbase.semver.implementation;

/**
 * Lançado para indicar um erro durante a análise.
 */
public class ParseException extends RuntimeException {

    /**
     * Constrói uma instância {@code ParseException} sem mensagem de erro.
     */
    public ParseException() {
        super();
    }

    /**
     * Constrói uma instância {@code ParseException} com uma mensagem de erro.
     *
     * @param message a mensagem de erro
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Constrói uma instância {@code ParseException} com uma mensagem de erro
     * e a exceção de causa.
     *
     * @param message a mensagem de erro
     * @param cause   uma exceção que causou esta exceção
     */
    public ParseException(String message, UnexpectedCharacterException cause) {
        super(message);
        initCause(cause);
    }

    /**
     * Retorna a representação de string desta exceção.
     *
     * @return a representação de string desta exceção
     */
    @Override
    public String toString() {
        Throwable cause = getCause();
        String msg = getMessage();
        if (msg != null) {
            msg += ((cause != null) ? " (" + cause.toString() + ")" : "");
            return msg;
        }
        return ((cause != null) ? cause.toString() : "");
    }
}
