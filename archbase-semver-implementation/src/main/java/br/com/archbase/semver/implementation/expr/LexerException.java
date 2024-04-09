package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.ParseException;

/**
 * Lançado durante a análise lexical quando
 * um caractere ilegal foi encontrado.
 */
public class LexerException extends ParseException {

    /**
     * A string sendo analisada a partir de um caractere ilegal.
     */
    private final String expr;

    /**
     * Constrói uma instância {@code LexerException} com
     * uma string começando com um caractere ilegal.
     *
     * @param expr a string começando com um caractere ilegal
     */
    LexerException(String expr) {
        this.expr = expr;
    }

    /**
     * Retorna a representação de string desta exceção.
     *
     * @return a representação de string desta exceção
     */
    @Override
    public String toString() {
        return "Carácter ilegal perto '" + expr + "'";
    }
}
