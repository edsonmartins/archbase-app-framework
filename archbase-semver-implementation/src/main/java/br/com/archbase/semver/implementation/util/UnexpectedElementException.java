package br.com.archbase.semver.implementation.util;

import java.util.Arrays;

/**
 * Lançado ao tentar consumir um elemento de fluxo de tipos inesperados.
 *
 * @see Stream#consume(Stream.ElementType...)
 */
public class UnexpectedElementException extends RuntimeException {

    /**
     * O elemento inesperado no fluxo.
     */
    private final transient Object unexpected;

    /**
     * A posição do elemento inesperado no fluxo.
     */
    private final int position;

    /**
     * A matriz dos tipos de elementos esperados.
     */
    private final transient Stream.ElementType<?>[] expected;

    /**
     * Constrói uma instância {@code UnexpectedElementException}
     * com o elemento inesperado e os tipos esperados.
     *
     * @param element  o elemento inesperado no stream
     * @param position a posição do elemento inesperado
     * @param expected uma matriz dos tipos de elementos esperados
     */
    UnexpectedElementException(
            Object element,
            int position,
            Stream.ElementType<?>... expected
    ) {
        unexpected = element;
        this.position = position;
        this.expected = expected;
    }

    /**
     * Obtém o elemento inesperado.
     *
     * @return o elemento inesperado
     */
    public Object getUnexpectedElement() {
        return unexpected;
    }

    /**
     * Obtém a posição do elemento inesperado.
     *
     * @return a posição do elemento inesperado
     */
    public int getPosition() {
        return position;
    }

    /**
     * Obtém os tipos de elemento esperados.
     *
     * @return um array de tipos de elementos esperados
     */
    public Stream.ElementType[] getExpectedElementTypes() {
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
                "Elemento inesperado '%s' na posição '%d'",
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
