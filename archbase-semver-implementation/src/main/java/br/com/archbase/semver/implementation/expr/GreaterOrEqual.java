package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador de comparação "maior ou igual a".
 */
class GreaterOrEqual implements Expression {

    /**
     * A versão analisada, o operando à direita
     * do operador "maior ou igual a".
     */
    private final Version parsedVersion;

    /**
     * Constructs a {@code GreaterOrEqual} expression with the parsed version.
     *
     * @param parsedVersion the parsed version
     */
    GreaterOrEqual(Version parsedVersion) {
        this.parsedVersion = parsedVersion;
    }

    /**
     * Verifica se a versão atual é superior
     * maior ou igual à versão analisada.
     *
     * @param version a versão com a qual comparar, o operando esquerdo
     *                do operador "maior ou igual a"
     * @return {@code true} se a versão for maior ou igual
     * para a versão analisada ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return version.greaterThanOrEqualTo(parsedVersion);
    }
}
