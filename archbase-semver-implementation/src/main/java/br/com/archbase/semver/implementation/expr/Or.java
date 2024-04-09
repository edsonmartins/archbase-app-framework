package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador lógico "ou".
 */
class Or implements Expression {

    /**
     * O operando de expressão à esquerda.
     */
    private final Expression left;

    /**
     * O operando da expressão à direita.
     */
    private final Expression right;

    /**
     * Constrói uma expressão {@code Or} com
     * os operandos da mão esquerda e da mão direita.
     *
     * @param left  o operando esquerdo da expressão
     * @param right o operando direito da expressão
     */
    Or(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Verifica se um dos operandos é avaliado como {@code true}.
     *
     * @param version a versão a ser interpretada
     * @return {@code true} se um dos operandos for avaliado como {@code true}
     * ou {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return left.interpret(version) || right.interpret(version);
    }
}
