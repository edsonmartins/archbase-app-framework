package br.com.archbase.controlplane.access;

import br.com.archbase.controlplane.session.AdminRole;

/**
 * Alcance exigido por {@link RequiresAdmin}.
 *
 * @since 3.4.6
 */
public enum AdminScope {

    /** Administra o tenant: {@code tenant_admin} ou {@code client_admin}. */
    TENANT(AdminRole.TENANT_ADMIN),
    /** Administra o cliente (todos os tenants dele): só {@code client_admin}. */
    CLIENT(AdminRole.CLIENT_ADMIN);

    private final AdminRole minimum;

    AdminScope(AdminRole minimum) {
        this.minimum = minimum;
    }

    public AdminRole minimum() {
        return minimum;
    }
}
