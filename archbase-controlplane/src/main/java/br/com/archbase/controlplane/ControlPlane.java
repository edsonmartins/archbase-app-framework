package br.com.archbase.controlplane;

import br.com.archbase.controlplane.api.Entitlements;
import br.com.archbase.controlplane.api.LicenseInfo;
import br.com.archbase.controlplane.api.Seats;
import br.com.archbase.controlplane.session.ControlPlaneAuthentication;
import br.com.archbase.controlplane.session.TenantSession;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import br.com.archbase.controlplane.tenant.TenantState;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Ponto de entrada do produto para o Control Plane.
 * <pre>{@code
 * TenantSession session = ControlPlane.currentSession();            // userId, email, tenantId, adminRole, seatId
 * boolean can   = ControlPlane.entitlements().can("powerview.export_excel");
 * long    limit = ControlPlane.entitlements().value("powerview.assentos");
 * Usage   usage = ControlPlane.entitlements().consume("copilot.ia_creditos", 120);
 * boolean seat  = ControlPlane.seats().check(userId);
 * TenantState state = ControlPlane.tenantState();                    // ACTIVE, DELINQUENT, SUSPENDED...
 * LicenseStatus lic = ControlPlane.license().status();               // VALID, GRACE, EXPIRED
 * }</pre>
 * Entitlements, assentos e estado são do tenant em {@code ArchbaseTenantContext} e vêm do cache local: nenhuma
 * dessas chamadas vai ao Control Plane. Em código com injeção de dependência, prefira injetar
 * {@link ControlPlaneState}.
 *
 * @since 3.4.6
 */
public final class ControlPlane {

    /** Estado da aplicação; reserva para código fora de requisição HTTP. */
    private static volatile ControlPlaneState state;

    /** Estado do contexto que atende a requisição atual — prevalece sobre {@link #state}. */
    private static final ThreadLocal<ControlPlaneState> REQUEST_STATE = new ThreadLocal<>();

    private ControlPlane() {
    }

    /** Ligado pela autoconfiguração; desligado quando o contexto fecha. */
    public static void bind(ControlPlaneState current) {
        state = current;
    }

    public static void unbind(ControlPlaneState current) {
        if (state == current) {
            state = null;
        }
    }

    /**
     * Fixa o estado para a requisição atual. Com mais de um contexto Spring na mesma JVM, o valor estático aponta
     * para o último que subiu; por requisição, cada contexto usa o seu.
     */
    public static void bindRequest(ControlPlaneState current) {
        REQUEST_STATE.set(current);
    }

    public static void unbindRequest() {
        REQUEST_STATE.remove();
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

    /** Entitlements do tenant atual. */
    public static Entitlements entitlements() {
        return new Entitlements(requireState(), ArchbaseTenantContext.getTenantId());
    }

    /** Assentos do tenant atual. */
    public static Seats seats() {
        return new Seats(requireState(), ArchbaseTenantContext.getTenantId());
    }

    /** Estado do tenant atual; {@link TenantState#UNKNOWN} sem informação do Control Plane. */
    public static TenantState tenantState() {
        return requireState().tenant(ArchbaseTenantContext.getTenantId())
                .map(TenantSnapshot::state).orElse(TenantState.UNKNOWN);
    }

    /** Licença da instância. */
    public static LicenseInfo license() {
        return new LicenseInfo(requireState());
    }

    private static ControlPlaneState requireState() {
        ControlPlaneState current = REQUEST_STATE.get();
        if (current == null) {
            current = state;
        }
        if (current == null) {
            throw new IllegalStateException("archbase-controlplane não está ligado (archbase.controlplane.enabled)");
        }
        return current;
    }
}
