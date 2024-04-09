package br.com.archbase.semver.implementation.util;


import br.com.archbase.semver.implementation.VersionParser;
import br.com.archbase.semver.implementation.expr.ExpressionParser;
import br.com.archbase.semver.implementation.expr.Lexer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Uma classe de fluxo simples usada para representar um fluxo de caracteres ou tokens.
 *
 * @param <E> o tipo de elementos mantidos neste fluxo
 * @see VersionParser
 * @see Lexer
 * @see ExpressionParser
 */
public class Stream<E> implements Iterable<E> {

    /**
     * A matriz que contém todos os elementos deste fluxo.
     */
    private final E[] elements;
    /**
     * O deslocamento atual que é incrementado quando um elemento é consumido.
     *
     * @see #consume()
     */
    private int offset = 0;

    /**
     * Constrói um fluxo contendo os elementos especificados.
     * <p>
     * O stream não armazena os elementos reais, mas a cópia defensiva.
     *
     * @param elements os elementos a serem transmitidos
     */
    public Stream(E[] elements) {
        this.elements = elements.clone();
    }

    /**
     * Consome o próximo elemento neste fluxo.
     *
     * @return o próximo elemento neste fluxo
     * ou {@code null} se não houver mais elementos restantes
     */
    public E consume() {
        if (offset >= elements.length) {
            return null;
        }
        return elements[offset++];
    }

    /**
     * Consome o próximo elemento neste fluxo
     * somente se for dos tipos esperados.
     *
     * @param <T>      representa o tipo de elemento deste fluxo, remove o
     *                 "criação de matriz genérica desmarcada para parâmetro varargs"
     *                 avisos
     * @param expected os tipos esperados
     * @return o próximo elemento neste fluxo
     * @throws UnexpectedElementException se o próximo elemento for de um tipo inesperado
     */
    public <T extends ElementType<E>> E consume(T... expected) {
        E lookahead = lookahead(1);
        for (ElementType<E> type : expected) {
            if (type.isMatchedBy(lookahead)) {
                return consume();
            }
        }
        throw new UnexpectedElementException(lookahead, offset, expected);
    }

    /**
     * Empurra um elemento de cada vez.
     */
    public void pushBack() {
        if (offset > 0) {
            offset--;
        }
    }

    /**
     * Retorna o próximo elemento neste fluxo sem consumi-lo.
     *
     * @return o próximo elemento neste fluxo
     */
    public E lookahead() {
        return lookahead(1);
    }

    /**
     * Retorna o elemento na posição especificada
     * neste fluxo sem consumi-lo.
     *
     * @param position a posição do elemento a retornar
     * @return o elemento na posição especificada
     * ou {@code null} se não houver mais elementos restantes
     */
    public E lookahead(int position) {
        int idx = offset + position - 1;
        if (idx < elements.length) {
            return elements[idx];
        }
        return null;
    }

    /**
     * Retorna o deslocamento atual deste fluxo.
     *
     * @return o deslocamento atual deste fluxo
     */
    public int currentOffset() {
        return offset;
    }

    /**
     * Verifica se o próximo elemento neste fluxo é dos tipos esperados.
     *
     * @param <T>      representa o tipo de elemento deste fluxo, remove os
     *                 "criação de matriz genérica desmarcada para parâmetro varargs"
     *                 avisos
     * @param expected os tipos esperados
     * @return {@code true} se o próximo elemento for do tipo esperado
     * ou {@code false} caso contrário
     */
    public <T extends ElementType<E>> boolean positiveLookahead(T... expected) {
        for (ElementType<E> type : expected) {
            if (type.isMatchedBy(lookahead(1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se existe um elemento neste fluxo de
     * os tipos esperados antes do tipo especificado.
     *
     * @param <T>      representa o tipo de elemento deste fluxo, remove os
     *                 "criação de matriz genérica desmarcada para parâmetro varargs"
     *                 avisos
     * @param before   do tipo antes do qual pesquisar
     * @param expected os tipos esperados
     * @return {@code true} se houver um elemento dos tipos esperados
     * antes do tipo especificado ou {@code false} caso contrário
     */
    public <T extends ElementType<E>> boolean positiveLookaheadBefore(
            ElementType<E> before,
            T... expected
    ) {
        E lookahead;
        for (int i = 1; i <= elements.length; i++) {
            lookahead = lookahead(i);
            if (before.isMatchedBy(lookahead)) {
                break;
            }
            for (ElementType<E> type : expected) {
                if (type.isMatchedBy(lookahead)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica se há um elemento neste fluxo de
     * os tipos esperados até a posição especificada.
     *
     * @param <T>      representa o tipo de elemento deste fluxo, remove os
     *                 "criação de matriz genérica desmarcada para parâmetro varargs"
     *                 avisos
     * @param until    a posição até a qual pesquisar
     * @param expected os tipos esperados
     * @return {@code true} se houver um elemento dos tipos esperados
     * até a posição especificada ou {@code false} caso contrário
     */
    public <T extends ElementType<E>> boolean positiveLookaheadUntil(
            int until,
            T... expected
    ) {
        for (int i = 1; i <= until; i++) {
            for (ElementType<E> type : expected) {
                if (type.isMatchedBy(lookahead(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retorna um iterador sobre os elementos que são deixados neste fluxo.
     *
     * @return um iterador dos elementos restantes neste fluxo
     */
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {

            /**
             * O índice para indicar a posição atual
             * deste iterador.
             *
             * O ponto de partida é definido para o atual
             * valor do deslocamento deste fluxo, de modo que
             * não itera sobre os elementos consumidos.
             */
            private int index = offset;

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() {
                return index < elements.length;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public E next() {
                if (index >= elements.length) {
                    throw new NoSuchElementException();
                }
                return elements[index++];
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Retorna um array contendo todos os
     * elementos que são deixados neste fluxo.
     * <p>
     * O array retornado é uma cópia segura.
     *
     * @return um array contendo todos os elementos neste stream
     */
    public E[] toArray() {
        return Arrays.copyOfRange(elements, offset, elements.length);
    }

    /**
     * A interface {@code ElementType} representa os tipos dos elementos
     * mantido por este fluxo e pode ser usado para filtragem de fluxo.
     *
     * @param <E> tipo de elementos mantidos por este fluxo
     */
    public static interface ElementType<E> {

        /**
         * Verifica se o elemento especificado corresponde a este tipo.
         *
         * @param element o elemento a ser testado
         * @return {@code true} se o elemento corresponder a este tipo
         * ou {@code false} caso contrário
         */
        boolean isMatchedBy(E element);
    }
}
