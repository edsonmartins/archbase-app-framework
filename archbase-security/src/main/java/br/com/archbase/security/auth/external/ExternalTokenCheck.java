package br.com.archbase.security.auth.external;

/**
 * Resultado de {@link ExternalBearerAuthenticator#check}: o token foi aceito para um tenant, ou recusado com um
 * status HTTP e um código estável que o cliente pode usar para escolher a mensagem.
 *
 * @since 3.4.6
 */
public final class ExternalTokenCheck {

    private final boolean accepted;
    private final String tenantId;
    private final Object claims;
    private final int status;
    private final String code;
    private final String message;

    private ExternalTokenCheck(boolean accepted, String tenantId, Object claims, int status, String code,
                               String message) {
        this.accepted = accepted;
        this.tenantId = tenantId;
        this.claims = claims;
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /**
     * Token válido.
     *
     * @param tenantId tenant do token; {@code null} só quando o autenticador aceita o tenant do {@code X-TENANT-ID}
     *                 (o filtro então exige o cabeçalho e recusa a requisição sem ele)
     * @param claims   o que o autenticador precisar levar para {@link ExternalBearerAuthenticator#authenticate}
     */
    public static ExternalTokenCheck accepted(String tenantId, Object claims) {
        return new ExternalTokenCheck(true, tenantId, claims, 0, null, null);
    }

    /** Token recusado: o filtro responde {@code status} com {@code {"code": ..., "message": ...}}. */
    public static ExternalTokenCheck rejected(int status, String code, String message) {
        return new ExternalTokenCheck(false, null, null, status, code, message);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String tenantId() {
        return tenantId;
    }

    public Object claims() {
        return claims;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
