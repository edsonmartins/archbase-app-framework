package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;

/**
 * Expressão para o operador lógico de "negação".
 */
class Not implements Expression {

    /**
     * A expressão a negar.
     */
    private final Expression expr;

    /**
     * Constructs a {@code Not} expression with an expression to negate.
     *
     * @param expr the expression to negate
     */
    Not(Expression expr) {
        this.expr = expr;
    }

    /**
     * Nega a expressão fornecida.
     *
     * @param version a versão a ser interpretada
     * @return {@code true} se a expressão fornecida for avaliada como
     * {@code false} e {@code false} caso contrário
     */
    @Override
    public boolean interpret(Version version) {
        return !expr.interpret(version);
    }
}
