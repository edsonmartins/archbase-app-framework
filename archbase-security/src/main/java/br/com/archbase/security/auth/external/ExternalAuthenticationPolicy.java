package br.com.archbase.security.auth.external;

/**
 * Política que um provedor de autenticação externo impõe ao {@code ArchbaseJwtAuthenticationFilter}. Sem bean deste
 * tipo, vale o comportamento de sempre (os dois métodos devolvem {@code true}).
 *
 * @since 3.4.6
 */
public interface ExternalAuthenticationPolicy {

    /**
     * Se o JWT próprio do Archbase (HS256, conferido em {@code SEGURANCA_TOKEN_ACESSO}) ainda é aceito. Com
     * {@code false}, o filtro ignora esse token e a requisição segue sem autenticação. Não afeta o token de API.
     */
    boolean acceptLegacyJwt();

    /**
     * Se o filtro pode usar {@code archbase.app.tenant.default.id} quando a requisição não traz tenant. Com
     * {@code false}, a requisição sem tenant segue sem tenant no contexto.
     */
    boolean allowDefaultTenant();
}
