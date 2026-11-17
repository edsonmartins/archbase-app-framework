package br.com.archbase.controlplane;

import br.com.archbase.controlplane.session.ControlPlaneAuthentication;
import br.com.archbase.controlplane.session.TenantSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Ponto de entrada do produto para o Control Plane.
 * <pre>{@code
 * TenantSession session = ControlPlane.currentSession(); // userId, email, tenantId, adminRole, seatId
 * }</pre>
 * Entitlements, assentos, estado do tenant e licença entram na Fase B.
 *
 * @since 3.4.6
 */
public final class ControlPlane {

    private ControlPlane() {
    }

    /**
     * Sessão do Control Plane da requisição atual.
     *
     * @throws IllegalStateException se a requisição não foi autenticada por token do Control Plane
     */
    public static TenantSession currentSession() {
        return findCurrentSession().orElseThrow(() -> new IllegalStateException(
                "A requisição atual não foi autenticada por token do Control Plane"));
    }

    /** Sessão do Control Plane da requisição atual, se houver. */
    public static Optional<TenantSession> findCurrentSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ControlPlaneAuthentication cp) {
            return Optional.ofNullable(cp.getSession());
        }
        return Optional.empty();
    }
}
