package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.ParseException;
import br.com.archbase.semver.implementation.UnexpectedCharacterException;
import br.com.archbase.semver.implementation.Version;

/**
 * Esta classe implementa DSL interno para o
 * Expressões SemVer usando interface fluente.
 */
public class CompositeExpression implements Expression {

    /**
     * A árvore de expressão subjacente.
     */
    private Expression exprTree;

    /**
     * Constrói uma {@code CompositeExpression}
     * com uma {@code Expression} subjacente.
     *
     * @param expr a expressão subjacente
     */
    public CompositeExpression(Expression expr) {
        exprTree = expr;
    }

    /**
     * Adiciona outra {@code Expression} a {@code CompositeExpression}
     * usando a expressão lógica {@code And}.
     *
     * @param expr uma expressão a ser adicionada
     * @return este {@code CompositeExpression}
     */
    public CompositeExpression and(Expression expr) {
        exprTree = new And(exprTree, expr);
        return this;
    }

    /**
     * Adiciona outra {@code Expression} a {@code CompositeExpression}
     * usando a expressão lógica {@code Or}.
     *
     * @param expr uma expressão a ser adicionada
     * @return este {@code CompositeExpression}
     */
    public CompositeExpression or(Expression expr) {
        exprTree = new Or(exprTree, expr);
        return this;
    }

    /**
     * Interpreta a expressão.
     *
     * @param version uma string {@code Version} para interpretar
     * @return o resultado da interpretação da expressão
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando uma string de versão inválida é fornecida
     * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
     */
    public boolean interpret(String version) {
        return interpret(Version.valueOf(version));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean interpret(Version version) {
        return exprTree.interpret(version);
    }

    /**
     * Uma classe com métodos auxiliares estáticos.
     */
    public static class Helper {

        private Helper() {
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code Not} subjacente.
         *
         * @param expr an {@code Expression} para negar
         * @return um {@code CompositeExpression} recém-criado
         */
        public static CompositeExpression not(Expression expr) {
            return new CompositeExpression(new Not(expr));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code Equal} subjacente.
         *
         * @param version a {@code Version} para verificar a igualdade
         * @return um {@code CompositeExpression} recém-criado
         */
        public static CompositeExpression eq(Version version) {
            return new CompositeExpression(new Equal(version));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code Equal} subjacente.
         *
         * @param version uma string {@code Version} para verificar a igualdade
         * @return um {@code CompositeExpression} recém-criado
         * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
         * @throws ParseException               quando uma string de versão inválida é fornecida
         * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
         */
        public static CompositeExpression eq(String version) {
            return eq(Version.valueOf(version));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code NotEqual} subjacente.
         *
         * @param version a {@code Version} para verificar a não igualdade
         * @return um {@code CompositeExpression} recém-criado
         */
        public static CompositeExpression neq(Version version) {
            return new CompositeExpression(new NotEqual(version));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code NotEqual} subjacente.
         *
         * @param version uma string {@code Version} para verificar a não igualdade
         * @return um {@code CompositeExpression} recém-criado
         * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
         * @throws ParseException               quando uma string de versão inválida é fornecida
         * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
         */
        public static CompositeExpression neq(String version) {
            return neq(Version.valueOf(version));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code Greater} subjacente.
         *
         * @param version a {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         */
        public static CompositeExpression gt(Version version) {
            return new CompositeExpression(new Greater(version));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code Greater} subjacente.
         *
         * @param version uma string {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
         * @throws ParseException               quando uma string de versão inválida é fornecida
         * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
         */
        public static CompositeExpression gt(String version) {
            return gt(Version.valueOf(version));
        }

        /**
         * Cria um {@code CompositeExpression} com um
         * expressão {@code GreaterOrEqual} subjacente.
         *
         * @param version a {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         */
        public static CompositeExpression gte(Version version) {
            return new CompositeExpression(new GreaterOrEqual(version));
        }

        /**
         * Cria um {@code CompositeExpression} com um
         * expressão {@code GreaterOrEqual} subjacente.
         *
         * @param version uma string {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
         * @throws ParseException               quando uma string de versão inválida é fornecida
         * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
         */
        public static CompositeExpression gte(String version) {
            return gte(Version.valueOf(version));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code Less} subjacente.
         *
         * @param version a {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         */
        public static CompositeExpression lt(Version version) {
            return new CompositeExpression(new Less(version));
        }

        /**
         * Cria uma {@code CompositeExpression} com
         * uma expressão {@code Less} subjacente.
         *
         * @param version uma string {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
         * @throws ParseException               quando uma string de versão inválida é fornecida
         * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
         */
        public static CompositeExpression lt(String version) {
            return lt(Version.valueOf(version));
        }

        /**
         * Cria um {@code CompositeExpression} com um
         * expressão {@code LessOrEqual} subjacente.
         *
         * @param version a {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         */
        public static CompositeExpression lte(Version version) {
            return new CompositeExpression(new LessOrEqual(version));
        }

        /**
         * Cria um {@code CompositeExpression} com um
         * expressão {@code LessOrEqual} subjacente.
         *
         * @param version uma string {@code Version} para comparar com
         * @return um {@code CompositeExpression} recém-criado
         * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
         * @throws ParseException               quando uma string de versão inválida é fornecida
         * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
         */
        public static CompositeExpression lte(String version) {
            return lte(Version.valueOf(version));
        }
    }
}
