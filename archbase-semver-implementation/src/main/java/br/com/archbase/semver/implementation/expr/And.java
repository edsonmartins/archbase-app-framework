package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador lógico "e".
 */
class And implements Expression {

    /**
     * O operando de expressão à esquerda.
     */
    private final Expression left;

    /**
     * O operando da expressão à direita.
     */
    private final Expression right;

    /**
     * Constrói uma expressão {@code And} com
     * os operandos da mão esquerda e da mão direita.
     *
     * @param left  o operando esquerdo da expressão
     * @param right o operando direito da expressão
     */
    And(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Verifica se ambos os operandos são avaliados como {@code true}.
     *
     * @param version a versão a ser interpretada
     * @return {@code true} se ambos os operandos forem avaliados como {@code true}
     * ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return left.interpret(version) && right.interpret(version);
    }
}
