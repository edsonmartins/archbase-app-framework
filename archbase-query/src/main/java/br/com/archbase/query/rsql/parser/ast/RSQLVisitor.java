package br.com.archbase.query.rsql.parser.ast;

/**
 * Uma interface para visitar os nós AST do RSQL.
 *
 * @param <R> Tipo de retorno do método do visitante.
 * @param <A> Tipo de um parâmetro opcional passado ao método do visitante.
 */
public interface RSQLVisitor<R, A> {

    R visit(AndNode node, A param);

    R visit(OrNode node, A param);

    R visit(ComparisonNode node, A param);
}
