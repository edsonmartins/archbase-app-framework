package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador de comparação "menor que".
 */
class Less implements Expression {

    /**
     * The parsed version, the right-hand
     * operand of the "less than" operator.
     */
    private final Version parsedVersion;

    /**
     * Constrói uma expressão {@code Less} com a versão analisada.
     *
     * @param parsedVersion a versão analisada
     */
    Less(Version parsedVersion) {
        this.parsedVersion = parsedVersion;
    }

    /**
     * Verifica se a versão atual é inferior à versão analisada.
     *
     * @param version a versão para comparar, o lado esquerdo
     *                operando do operador "menor que"
     * @return {@code true} se a versão for menor que o
     * versão analisada ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return version.lessThan(parsedVersion);
    }
}
