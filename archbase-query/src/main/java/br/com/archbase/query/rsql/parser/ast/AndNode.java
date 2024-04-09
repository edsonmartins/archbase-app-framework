package br.com.archbase.query.rsql.parser.ast;


import java.util.List;


public final class AndNode extends LogicalNode {

    public AndNode(List<? extends Node> children) {
        super(LogicalOperator.AND, children);
    }

    public AndNode withChildren(List<? extends Node> children) {
        return new AndNode(children);
    }

    public <R, A> R accept(RSQLVisitor<R, A> visitor, A param) {
        return visitor.visit(this, param);
    }
}
