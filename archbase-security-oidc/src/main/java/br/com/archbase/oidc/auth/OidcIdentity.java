package br.com.archbase.oidc.auth;

import java.util.Map;

/**
 * Quem o IdP diz que é o usuário, já validado e mapeado pela configuração do provedor.
 *
 * @param providerId    id do provedor na configuração
 * @param subject       identificador estável no IdP (o claim configurado em {@code claims.subject})
 * @param tenantId      tenant do Archbase vindo do token ({@code null} quando a fonte é o {@code X-TENANT-ID})
 * @param claims        todos os claims do token validado
 * @since 3.5.0
 */
public record OidcIdentity(String providerId, String subject, String email, boolean emailVerified, String name,
                           String tenantId, Map<String, Object> claims) {

    public OidcIdentity {
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }
}
