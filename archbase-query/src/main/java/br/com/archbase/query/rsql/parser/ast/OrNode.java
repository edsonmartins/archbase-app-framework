package br.com.archbase.query.rsql.parser.ast;

import java.util.List;

public final class OrNode extends LogicalNode {

    public OrNode(List<? extends Node> children) {
        super(LogicalOperator.OR, children);
    }

    public OrNode withChildren(List<? extends Node> children) {
        return new OrNode(children);
    }

    public <R, A> R accept(RSQLVisitor<R, A> visitor, A param) {
        return visitor.visit(this, param);
    }
}
