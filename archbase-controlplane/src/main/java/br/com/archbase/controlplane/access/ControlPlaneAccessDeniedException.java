package br.com.archbase.controlplane.access;

/**
 * Recusa de acesso por regra do Control Plane, com código estável para o front escolher a mensagem:
 * {@code NOT_ADMIN}, {@code NOT_ENTITLED}, {@code NO_SEAT}, {@code TENANT_SUSPENDED} e {@code LICENSE_EXPIRED}.
 * Vira 403 em {@link ControlPlaneExceptionHandler}.
 *
 * @since 3.4.6
 */
public class ControlPlaneAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String NOT_ADMIN = "NOT_ADMIN";
    public static final String NOT_ENTITLED = "NOT_ENTITLED";
    public static final String NO_SEAT = "NO_SEAT";
    public static final String TENANT_SUSPENDED = "TENANT_SUSPENDED";
    public static final String LICENSE_EXPIRED = "LICENSE_EXPIRED";

    private final String code;

    public ControlPlaneAccessDeniedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
