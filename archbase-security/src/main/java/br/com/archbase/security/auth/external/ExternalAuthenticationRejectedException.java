package br.com.archbase.security.auth.external;

/**
 * Lançada por {@link ExternalBearerAuthenticator#authenticate} para recusar um token válido por motivo do produto
 * (ex.: usuário local desativado). O filtro responde {@link #getStatus()} com {@code {"code": ..., "message": ...}}.
 *
 * @since 3.4.6
 */
public class ExternalAuthenticationRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String code;

    public ExternalAuthenticationRejectedException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
