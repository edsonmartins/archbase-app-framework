package br.com.archbase.controlplane.api;

import br.com.archbase.controlplane.state.ControlPlaneState;

/**
 * Assentos de um tenant.
 *
 * @since 3.4.6
 */
public class Seats {

    private final ControlPlaneState state;
    private final String tenantId;

    public Seats(ControlPlaneState state, String tenantId) {
        this.state = state;
        this.tenantId = tenantId;
    }

    /**
     * Se o usuário (id do Control Plane) tem assento no tenant, segundo o último snapshot. Sem informação de assentos
     * do Control Plane, devolve {@code false}: quem precisa decidir por requisição usa o claim {@code seat} do token
     * (ver {@code @RequiresSeat}).
     */
    public boolean check(String userId) {
        return state.tenant(tenantId)
                .filter(s -> s.seatsKnown())
                .map(s -> s.seatHolders().containsKey(userId))
                .orElse(false);
    }
}
