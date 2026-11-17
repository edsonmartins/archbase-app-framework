package br.com.archbase.controlplane.token;

/**
 * Resultado da validação: as claims, ou um código estável de recusa com o status HTTP correspondente.
 *
 * @since 3.4.6
 */
public record TokenValidationResult(ControlPlaneClaims claims, int status, String code, String message) {

    /** Token vencido. */
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    /** Assinatura não confere com a chave do {@code kid}. */
    public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
    /** {@code kid} ausente ou desconhecido mesmo depois de buscar o JWKS. */
    public static final String UNKNOWN_KEY = "UNKNOWN_KEY";
    /** {@code aud} não contém a URL pública desta instância. */
    public static final String WRONG_AUDIENCE = "WRONG_AUDIENCE";
    /** {@code iss} diferente do Control Plane configurado. */
    public static final String WRONG_ISSUER = "WRONG_ISSUER";
    /** Token de sessão sem {@code tnt}. */
    public static final String TENANT_REQUIRED = "TENANT_REQUIRED";
    /** Produto exige assento e o token não traz {@code seat}. */
    public static final String NO_SEAT = "NO_SEAT";
    /** Qualquer outra falha (formato, claim obrigatório ausente, valor desconhecido). */
    public static final String INVALID_TOKEN = "INVALID_TOKEN";

    public static TokenValidationResult valid(ControlPlaneClaims claims) {
        return new TokenValidationResult(claims, 200, null, null);
    }

    public static TokenValidationResult rejected(int status, String code, String message) {
        return new TokenValidationResult(null, status, code, message);
    }

    public boolean isValid() {
        return claims != null;
    }
}
