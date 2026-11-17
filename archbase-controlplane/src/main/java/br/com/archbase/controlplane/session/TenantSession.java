package br.com.archbase.controlplane.session;

import java.time.Instant;

/**
 * Quem está na requisição, segundo o token do Control Plane.
 *
 * @param userId      {@code sub}: id do usuário no Control Plane (não o e-mail); no token de instância, o id da
 *                    instância
 * @param email       e-mail verificado; {@code null} no token de instância
 * @param tenantId    tenant da requisição
 * @param adminRole   papel administrativo; {@code null} no token de instância
 * @param seatId      assento, ou {@code null}
 * @param localUserId id do usuário local do produto a que o {@code sub} foi vinculado, ou {@code null}
 * @param instance    se a credencial é um token de instância (serviço a serviço)
 * @param tokenId     {@code jti}, ou {@code null}
 * @param expiresAt   {@code exp}
 * @since 3.4.6
 */
public record TenantSession(
        String userId,
        String email,
        String tenantId,
        AdminRole adminRole,
        String seatId,
        String localUserId,
        boolean instance,
        String tokenId,
        Instant expiresAt) {

    /** Se o token trouxe assento. */
    public boolean hasSeat() {
        return seatId != null;
    }
}
