package br.com.archbase.analytics.port;

import java.time.Instant;

/**
 * Auditoria da consulta ad-hoc (a consulta livre não tem verbo e não gera
 * rastro no modelo de auditoria por ação; sem isto, perde-se "quem viu o quê").
 *
 * <p>O framework chama este método a cada consulta; o produto decide a
 * persistência. O <b>conjunto de resultados NUNCA</b> passa por aqui — só a
 * estrutura da consulta e os números do atendimento.
 */
public interface AnalyticsAuditPort {

    record AuditedQuery(
            String username,
            /** Hash do contexto de segurança — identifica o escopo sem copiar as claims. */
            String securityContextHash,
            /** Estrutura da consulta (JSON), nunca o resultado. */
            String queryJson,
            /** explorer | widget. */
            String origin,
            /** load | meta. */
            String endpoint,
            Instant startedAt,
            Integer durationMs,
            Integer rowCount,
            boolean truncated,
            /** ok | erro. */
            String outcome,
            /** Código fechado quando erro; nulo em sucesso. */
            String errorCode) {
    }

    void record(AuditedQuery query);
}
