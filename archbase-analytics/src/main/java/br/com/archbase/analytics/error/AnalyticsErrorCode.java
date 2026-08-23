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
    UPSTREAM_ERROR(null);

    /** {@code retryable} do envelope; nulo quando o campo é omitido. */
    private final Boolean retryable;

    AnalyticsErrorCode(Boolean retryable) {
        this.retryable = retryable;
    }

    public Boolean retryable() {
        return retryable;
    }
}
