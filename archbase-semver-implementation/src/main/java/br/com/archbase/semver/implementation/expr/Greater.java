package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador de comparação "maior que".
 */
class Greater implements Expression {

    /**
     * A versão analisada, a mão direita
     * operando do operador "maior que".
     */
    private final Version parsedVersion;

    /**
     * Constrói uma expressão {@code Greater} com a versão analisada.
     *
     * @param parsedVersion a versão analisada
     */
    Greater(Version parsedVersion) {
        this.parsedVersion = parsedVersion;
    }

    /**
     * Verifica se a versão atual é superior à versão analisada.
     *
     * @param version a versão para comparar, o lado esquerdo
     *                operando do operador "maior que"
     * @return {@code true} se a versão for maior que o
     * versão analisada ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return version.greaterThan(parsedVersion);
    }
}
