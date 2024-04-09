package br.com.archbase.semver.implementation;

import br.com.archbase.semver.implementation.util.UnexpectedElementException;

import java.util.Arrays;

/**
 * Lançado ao tentar consumir um caracter de tipos inesperados.
 * <p>
 * Esta exceção é uma exceção de wrapper que estende {@code ParseException}.
 */
public class UnexpectedCharacterException extends ParseException {

    /**
     * O character inesperado.
     */
    private final Character unexpected;

    /**
     * A posição do character inesperado.
     */
    private final int position;

    /**
     * A matriz de tipos de caracteres esperados.
     */
    private final VersionParser.CharType[] expected;

    /**
     * Constrói uma instância {@code UnexpectedCharacterException} com
     * a exceção agrupada {@code UnexpectedElementException}.
     *
     * @param cause a exceção encapsulada
     */
    UnexpectedCharacterException(UnexpectedElementException cause) {
        position = cause.getPosition();
        unexpected = (Character) cause.getUnexpectedElement();
        expected = (VersionParser.CharType[]) cause.getExpectedElementTypes();
    }

    /**
     * Constrói uma instância {@code UnexpectedCharacterException}
     * com o caráter inesperado, sua posição e os tipos esperados.
     *
     * @param unexpected o caracter inesperado
     * @param position   a posição do caracter inesperado
     * @param expected   uma matriz dos tipos de caracteres esperados
     */
    UnexpectedCharacterException(
            Character unexpected,
            int position,
            VersionParser.CharType... expected
    ) {
        this.unexpected = unexpected;
        this.position = position;
        this.expected = expected;
    }

    /**
     * Obtém o caracter inesperado.
     *
     * @return o caracter inesperado
     */
    Character getUnexpectedCharacter() {
        return unexpected;
    }

    /**
     * Obtém a posição do caracter inesperado.
     *
     * @return a posição do caracter inesperado
     */
    int getPosition() {
        return position;
    }

    /**
     * Obtém os tipos de caracteres esperados.
     *
     * @return uma matriz de tipos de caracteres esperados
     */
    VersionParser.CharType[] getExpectedCharTypes() {
        return expected;
    }

    /**
     * Retorna a representação de string desta exceção
     * contendo informações sobre o inesperado
     * elemento e, se disponível, sobre os tipos esperados.
     *
     * @return a representação de string desta exceção
     */
    @Override
    public String toString() {
        String message = String.format(
                "Character inesperado '%s (%s)' na posição '%d'",
                VersionParser.CharType.forCharacter(unexpected),
                unexpected,
                position
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
