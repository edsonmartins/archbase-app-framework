package br.com.archbase.query.rsql.parser.ast;

/**
 * Interface comum dos nós AST. As implementações devem ser imutáveis.
 */
public interface Node {

    /**
     * Aceita o visitante, chama seu <tt>visit()</tt> método e retorna um resultado.
     *
     * <p>Cada implementação deve implementar esses métodos exatamente como listados:
     * <pre>{@code
     * public <R, A> R accept(RSQLVisitor<R, A> visitor, A param) {
     *     return visitor.visit(this, param);
     * }
     * }</pre>
     *
     * @param visitor O visitante cujo método apropriado será chamado.
     * @param param   Um parâmetro opcional para passar ao visitante.
     * @param <R>     Tipo de retorno do método do visitante.
     * @param <A>     Tipo de um parâmetro opcional passado ao método do visitante.
     * @return Um objeto retornado pelo visitante (may be <tt>null</tt>).
     */
    <R, A> R accept(RSQLVisitor<R, A> visitor, A param);

    /**
     * Aceita o visitante, chama seu <tt>visit()</tt> método e retorna o resultado.
     * <p>
     * Este método deve apenas chamar {@link #accept(RSQLVisitor, Object)} com
     * <tt>null</tt> como o segundo argumento.
     */
    <R, A> R accept(RSQLVisitor<R, A> visitor);
}
