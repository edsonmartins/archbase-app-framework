package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador de comparação "diferente".
 */
class NotEqual implements Expression {

    /**
     * A versão analisada, o operando à direita do operador "diferente".
     */
    private final Version parsedVersion;

    /**
     * Constructs a {@code NotEqual} expression with the parsed version.
     *
     * @param parsedVersion the parsed version
     */
    NotEqual(Version parsedVersion) {
        this.parsedVersion = parsedVersion;
    }

    /**
     * Verifica se a versão atual não é igual à versão analisada.
     *
     * @param version a versão com a qual comparar, o lado esquerdo
     *                operando do operador "diferente"
     * @return {@code true} se a versão não for igual ao
     * versão analisada ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return !version.equals(parsedVersion);
    }
}
