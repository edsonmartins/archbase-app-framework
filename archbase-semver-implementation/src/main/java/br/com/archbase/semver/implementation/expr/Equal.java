package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador de comparação "igual".
 */
class Equal implements Expression {

    /**
     * A versão analisada, o operando à direita do operador "igual".
     */
    private final Version parsedVersion;

    /**
     * Constrói uma expressão {@code Equal} com a versão analisada.
     *
     * @param parsedVersion a versão analisada
     */
    Equal(Version parsedVersion) {
        this.parsedVersion = parsedVersion;
    }

    /**
     * Verifica se a versão atual é igual à versão analisada.
     *
     * @param version a versão para comparar, o lado esquerdo
     *                operando do operador "igual"
     * @return {@code true} se a versão for igual a
     * versão analisada ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return version.equals(parsedVersion);
    }
}
