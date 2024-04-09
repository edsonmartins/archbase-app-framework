package br.com.archbase.query.rsql.parser.ast;

/**
 * Um adaptador para a interface {@link RSQLVisitor} com um contrato mais simples que omite o opcional
 * segundo argumento.
 *
 * @param <R> Tipo de retorno do método do visitante.
 */
@SuppressWarnings("java:S1172")
public interface NoArgRSQLVisitorAdapter<R> extends RSQLVisitor<R, Void> {

    public R visit(AndNode node);

    public R visit(OrNode node);

    public R visit(ComparisonNode node);


    public default R visit(AndNode node, Void param) {
        return visit(node);
    }

    public default R visit(OrNode node, Void param) {
        return visit(node);
    }

    public default R visit(ComparisonNode node, Void param) {
        return visit(node);
    }
}
