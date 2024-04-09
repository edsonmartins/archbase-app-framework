package br.com.archbase.semver.implementation;

/**
 * Uma interface de analisador.
 *
 * @param <T> o tipo de saída do analisador
 */
public interface Parser<T> {

    /**
     * Analisa a string de entrada.
     *
     * @param input a string a ser analisada
     * @return the Abstract Syntax Tree
     */
    T parse(String input);
}
