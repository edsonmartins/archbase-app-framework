package br.com.archbase.controlplane.access;

/**
 * Recusa de acesso por regra do Control Plane, com código estável para o front escolher a mensagem:
 * {@code NOT_ADMIN} nesta fase; {@code NO_SEAT}, {@code NOT_ENTITLED}, {@code TENANT_SUSPENDED} e
 * {@code LICENSE_EXPIRED} a partir da Fase B. Vira 403 em {@link ControlPlaneExceptionHandler}.
 *
 * @since 3.4.6
 */
public class ControlPlaneAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String NOT_ADMIN = "NOT_ADMIN";

    private final String code;

    public ControlPlaneAccessDeniedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
