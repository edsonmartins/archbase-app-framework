package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * A interface {@code Expression} deve ser implementada
 * pelos nós da Árvore Sintaxe Abstrata produzida por
 * a classe {@code ExpressionParser}.
 */
public interface Expression {

    /**
     * Interpreta a expressão.
     *
     * @param version a versão a ser interpretada
     * @return o resultado da interpretação da expressão
     */
    boolean interpret(Version version);
}
