package br.com.archbase.query.rsql.parser.ast;

@SuppressWarnings("all")
public abstract class AbstractNode implements Node {

    /**
     * Aceita o visitante, chama seu método visit () e retorna o resultado.
     * Este método apenas chama {@link #accept (RSQLVisitor, Object)} com
     * <tt> null </tt> como o segundo argumento.
     */
    public <R, A> R accept(RSQLVisitor<R, A> visitor) {
        return accept(visitor, null);
    }
}
