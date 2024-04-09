package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expression for the comparison "less than or equal to" operator.
 */
class LessOrEqual implements Expression {

    /**
     * A versão analisada, o operando à direita
     * do operador "menor ou igual a".
     */
    private final Version parsedVersion;

    /**
     * Constructs a {@code LessOrEqual} expression with the parsed version.
     *
     * @param parsedVersion the parsed version
     */
    LessOrEqual(Version parsedVersion) {
        this.parsedVersion = parsedVersion;
    }

    /**
     * Verifica se a versão atual é inferior
     * maior ou igual à versão analisada.
     *
     * @param version a versão com a qual comparar, o operando esquerdo
     *                do operador "menor ou igual a"
     * @return {@code true} se a versão for menor ou igual
     * para a versão analisada ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return version.lessThanOrEqualTo(parsedVersion);
    }
}
