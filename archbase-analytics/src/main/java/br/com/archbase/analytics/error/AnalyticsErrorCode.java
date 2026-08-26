package br.com.archbase.analytics.error;

/**
 * Conjunto FECHADO de códigos de falha que a biblioteca de frontend trata.
 *
 * <p>É o mesmo vocabulário do RFC de contrato de transporte da biblioteca — a
 * mensagem textual do servidor NUNCA chega à tela; só o código. Fonte única do
 * contrato, referenciada pelos dois lados do fio.
 */
public enum AnalyticsErrorCode {
    QUERY_TIMEOUT(false),
    CONCURRENCY_LIMIT(true),
    FORBIDDEN_MEMBER(null),
    UPSTREAM_ERROR(null),

    /**
     * Requisição sem usuário autenticado (401).
     *
     * <p>Acompanha o 401 de HTTP, que a biblioteca já trata como sessão expirada — o código existe
     * para que o vocabulário continue fechado, não porque a tela precise distingui-lo.
     */
    UNAUTHENTICATED(false),

    /**
     * O parâmetro {@code query} do GET não é JSON válido (400).
     *
     * <p>Antes esta requisição virava 502 {@code UPSTREAM_ERROR}, acusando o Cube de um erro que
     * nasceu na borda. O código novo diz de quem é o problema.
     */
    INVALID_QUERY(false);

    /** {@code retryable} do envelope; nulo quando o campo é omitido. */
    private final Boolean retryable;

    AnalyticsErrorCode(Boolean retryable) {
        this.retryable = retryable;
    }

    public Boolean retryable() {
        return retryable;
    }
}
