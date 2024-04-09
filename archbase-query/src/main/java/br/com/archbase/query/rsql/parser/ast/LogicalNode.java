package br.com.archbase.query.rsql.parser.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static br.com.archbase.query.rsql.parser.ast.StringUtils.join;
import static java.util.Collections.unmodifiableList;

/**
 * Superclasse de todos os nós lógicos que representam uma operação lógica que conecta
 * nós filhos.
 */
public abstract class LogicalNode extends AbstractNode implements Iterable<Node> {

    private final List<Node> children;

    private final LogicalOperator operator;


    /**
     * @param operator Nãp pode ser <tt>null</tt>.
     * @param children Nós filhos, ou seja, operandos; não deve ser <tt>null</tt>.
     */
    protected LogicalNode(LogicalOperator operator, List<? extends Node> children) {
        assert operator != null : "operador não deve ser nulo";
        assert children != null : "filhos não devem ser nulos";

        this.operator = operator;
        this.children = unmodifiableList(new ArrayList<>(children));
    }


    /**
     * Retorna uma cópia deste nó com os nós filhos especificados.
     */
    public abstract LogicalNode withChildren(List<? extends Node> children);


    /**
     * Iterar sobre nós filhos. A coleção subjacente não pode ser modificada!
     */
    public Iterator<Node> iterator() {
        return children.iterator();
    }

    public LogicalOperator getOperator() {
        return operator;
    }

    /**
     * Retorna uma cópia dos nós filhos.
     */
    public List<Node> getChildren() {
        return new ArrayList<>(children);
    }


    @Override
    public String toString() {
        return "(" + join(children, operator.toString()) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogicalNode)) return false;
        LogicalNode nodes = (LogicalNode) o;

        return children.equals(nodes.children)
                && operator == nodes.operator;
    }

    @Override
    public int hashCode() {
        int result = children.hashCode();
        result = 31 * result + operator.hashCode();
        return result;
    }
}
