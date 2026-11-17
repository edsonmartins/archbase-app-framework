package br.com.archbase.controlplane.session;

/**
 * Papel administrativo vindo do claim {@code adm} do token de sessão. Ordem crescente de alcance:
 * {@link #MEMBER} &lt; {@link #TENANT_ADMIN} &lt; {@link #CLIENT_ADMIN}.
 * <p>
 * Os papéis funcionais (vendedor, supervisor…) continuam no produto; este é só o papel no Control Plane.
 *
 * @since 3.4.6
 */
public enum AdminRole {

    MEMBER("member"),
    TENANT_ADMIN("tenant_admin"),
    CLIENT_ADMIN("client_admin");

    private final String claimValue;

    AdminRole(String claimValue) {
        this.claimValue = claimValue;
    }

    /** Valor no claim {@code adm}. */
    public String claimValue() {
        return claimValue;
    }

    /** Authority do Spring Security: {@code ROLE_CP_MEMBER}, {@code ROLE_CP_TENANT_ADMIN}, {@code ROLE_CP_CLIENT_ADMIN}. */
    public String authority() {
        return "ROLE_CP_" + name();
    }

    /** Se este papel alcança pelo menos o informado. */
    public boolean atLeast(AdminRole other) {
        return ordinal() >= other.ordinal();
    }

    /** O papel do claim; {@code null} se o valor for desconhecido. Ausente vira {@link #MEMBER}. */
    public static AdminRole fromClaim(String value) {
        if (value == null || value.isBlank()) {
            return MEMBER;
        }
        for (AdminRole role : values()) {
            if (role.claimValue.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return null;
    }
}
