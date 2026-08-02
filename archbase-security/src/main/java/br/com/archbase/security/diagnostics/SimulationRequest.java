package br.com.archbase.security.diagnostics;

/**
 * Pedido de simulação: quem, e o quê.
 *
 * <p>Identifica o alvo por {@code userId} ou por {@code email} — a ordem em que quem opera o admin
 * costuma ter o dado. Escopo em branco significa "sem restrição de escopo", que é como a maioria
 * das concessões existe.
 */
public record SimulationRequest(
        String userId,
        String email,
        String resource,
        String action,
        String tenantId,
        String companyId,
        String projectId) {
}
