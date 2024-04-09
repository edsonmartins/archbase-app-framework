package br.com.archbase.query.rsql.parser.ast;


import java.util.ArrayList;
import java.util.List;

import static br.com.archbase.query.rsql.parser.ast.StringUtils.join;


public final class ComparisonNode extends AbstractNode {

    private final ComparisonOperator operator;

    private final String selector;

    private final List<String> arguments;


    /**
     * @param operator  Não deve ser <tt>null</tt>.
     * @param selector  Não deve ser <tt>null</tt> ou em branco.
     * @param arguments Não deve ser <tt>null</tt> ou vazio. Se a operador não for
     *                  {@link ComparisonOperator#isMultiValue() multiValue}, então deve conter exatamente
     *                  um argumento.
     * @throws IllegalArgumentException Se uma das condições especificadas acima não for atendida.
     */
    public ComparisonNode(ComparisonOperator operator, String selector, List<String> arguments) {
        Assert.notNull(operator, "operador não deve ser nulo");
        Assert.notBlank(selector, "o seletor não deve estar em branco");
        Assert.notEmpty(arguments, "a lista de argumentos não deve estar vazia");
        Assert.isTrue(operator.isMultiValue() || arguments.size() == 1,
                "operador %s espera um único argumento, mas vários valores fornecidos", operator);

        this.operator = operator;
        this.selector = selector;
        this.arguments = new ArrayList<>(arguments);
    }


    public <R, A> R accept(RSQLVisitor<R, A> visitor, A param) {
        return visitor.visit(this, param);
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    /**
     * Retorna uma cópia deste nó com o operador especificado.
     *
     * @param newOperator Não deve ser <tt>null</tt>.
     */
    public ComparisonNode withOperator(ComparisonOperator newOperator) {
        return new ComparisonNode(newOperator, selector, arguments);
    }

    public String getSelector() {
        return selector;
    }

    /**
     * Retorna uma cópia deste nó com o seletor especificado.
     *
     * @param newSelector Não deve ser <tt>null</tt> ou em branco.
     */
    public ComparisonNode withSelector(String newSelector) {
        return new ComparisonNode(operator, newSelector, arguments);
    }

    /**
     * Retorna uma cópia da lista de argumentos. É garantido que contém pelo menos um item.
     * Quando o operador não é {@link ComparisonOperator # isMultiValue () multiValue}, então
     * contém exatamente um argumento.
     */
    public List<String> getArguments() {
        return new ArrayList<>(arguments);
    }

    /**
     * Retorna uma cópia deste nó com os argumentos especificados.
     *
     * @param newArguments Não deve ser <tt>null</tt> or vazio. Se a operador não for
     *                     {@link ComparisonOperator#isMultiValue() multiValue}, então deve conter exatamente
     *                     um argumento.
     */
    public ComparisonNode withArguments(List<String> newArguments) {
        return new ComparisonNode(operator, selector, newArguments);
    }


    @Override
    public String toString() {
        String args = arguments.size() > 1
                ? "('" + join(arguments, "','") + "')"
                : "'" + arguments.get(0) + "'";
        return selector + operator + args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComparisonNode)) return false;
        ComparisonNode that = (ComparisonNode) o;

        return arguments.equals(that.arguments)
                && operator.equals(that.operator)
                && selector.equals(that.selector);
    }

    @Override
    public int hashCode() {
        int result = selector.hashCode();
        result = 31 * result + arguments.hashCode();
        result = 31 * result + operator.hashCode();
        return result;
    }
}
