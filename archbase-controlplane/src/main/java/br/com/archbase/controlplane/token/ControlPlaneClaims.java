package br.com.archbase.controlplane.token;

import br.com.archbase.controlplane.session.AdminRole;

import java.time.Instant;

/**
 * Claims de um token do Control Plane já validado.
 *
 * @param use       sessão de usuário ou token de instância (serviço a serviço)
 * @param subject   {@code sub}: id do usuário no Control Plane (sessão) ou id da instância
 * @param email     {@code email} verificado (só sessão)
 * @param tenantId  {@code tnt}; obrigatório na sessão, opcional no token de instância
 * @param adminRole {@code adm} (sessão); {@code null} no token de instância
 * @param seatId    {@code seat}, ou {@code null}
 * @param tokenId   {@code jti}, ou {@code null}
 * @param keyId     {@code kid} do cabeçalho
 * @since 3.4.6
 */
public record ControlPlaneClaims(
        TokenUse use,
        String subject,
        String email,
        String tenantId,
        AdminRole adminRole,
        String seatId,
        String tokenId,
        String keyId,
        Instant issuedAt,
        Instant expiresAt) {

    /** Tipo do token, pelo claim {@code token_use} (mesmo nome usado pelo JWT do Archbase). */
    public enum TokenUse {
        /** {@code token_use=session} ou ausente: sessão de um usuário num tenant. */
        SESSION,
        /** {@code token_use=instance}: credencial da instância para chamadas serviço a serviço. */
        INSTANCE
    }
}
