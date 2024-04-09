package br.com.archbase.query.rsql.parser;

/**
 * Uma exceção de nível superior do analisador RSQL que envolve todas as exceções ocorridas na análise.
 */
public class RSQLParserException extends RuntimeException {

    public RSQLParserException() {
        super();
    }

    public RSQLParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RSQLParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public RSQLParserException(String message) {
        super(message);
    }

    public RSQLParserException(Throwable cause) {
        super(cause);
    }

}
