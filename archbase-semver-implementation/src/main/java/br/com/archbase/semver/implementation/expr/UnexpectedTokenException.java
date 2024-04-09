package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.ParseException;
import br.com.archbase.semver.implementation.expr.Lexer.Token;
import br.com.archbase.semver.implementation.util.UnexpectedElementException;

import java.util.Arrays;

/**
 * Lançado quando um token de tipos inesperados é encontrado durante a análise.
 */
public class UnexpectedTokenException extends ParseException {

    /**
     * O token inesperado.
     */
    private final transient Token unexpected;

    /**
     * A matriz dos tipos de token esperados.
     */
    private final Token.Type[] expected;

    /**
     * Constrói uma instância {@code UnexpectedTokenException} com
     * a exceção agrupada {@code UnexpectedElementException}.
     *
     * @param cause a exceção encapsulada
     */
    UnexpectedTokenException(UnexpectedElementException cause) {
        unexpected = (Token) cause.getUnexpectedElement();
        expected = (Token.Type[]) cause.getExpectedElementTypes();
    }

    /**
     * Constrói uma instância {@code UnexpectedTokenException}
     * com o token inesperado e os tipos esperados.
     *
     * @param token    o token inesperado
     * @param expected uma matriz dos tipos de token esperados
     */
    UnexpectedTokenException(Token token, Token.Type... expected) {
        unexpected = token;
        this.expected = expected;
    }

    /**
     * Obtém o token inesperado.
     *
     * @return o token inesperado
     */
    Token getUnexpectedToken() {
        return unexpected;
    }

    /**
     * Obtém os tipos de token esperados.
     *
     * @return uma matriz de tipos de token esperados
     */
    Token.Type[] getExpectedTokenTypes() {
        return expected;
    }

    /**
     * Retorna a representação de string desta exceção
     * contendo informações sobre o inesperado
     * token e, se disponível, sobre os tipos esperados.
     *
     * @return a representação de string desta exceção
     */
    @Override
    public String toString() {
        String message = String.format(
                "Token inesperado '%s'",
                unexpected
        );
        if (expected.length > 0) {
            message += String.format(
                    ", esperando '%s'",
                    Arrays.toString(expected)
            );
        }
        return message;
    }
}
