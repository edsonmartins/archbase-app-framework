package br.com.archbase.oidc.auth;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.provider.OidcIntrospector;
import br.com.archbase.oidc.provider.OidcProviderRegistry;
import br.com.archbase.oidc.token.ClaimReader;
import br.com.archbase.oidc.token.UnverifiedJwt;
import br.com.archbase.oidc.user.OidcIdentityLinker;
import br.com.archbase.oidc.user.OidcRoleSynchronizer;
import br.com.archbase.security.auth.external.ExternalAuthenticationRejectedException;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import br.com.archbase.security.auth.external.ExternalTokenCheck;
import br.com.archbase.security.persistence.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.util.Map;
import java.util.Optional;

/**
 * Autentica access tokens de IdP OIDC pelo SPI do {@code ArchbaseJwtAuthenticationFilter}.
 * <ul>
 * <li>{@link #supports}: o {@code iss} do token (lido sem verificar) pertence a um provedor configurado. Token de
 * outro emissor segue para os demais autenticadores e para o login próprio, como antes.</li>
 * <li>{@link #check}: assinatura (JWKS), algoritmo, emissor, {@code aud}, {@code exp}/{@code nbf}, claims exigidos;
 * mapeia a identidade e o tenant. Sem banco.</li>
 * <li>{@link #authenticate}: com o tenant já no contexto, encontra o usuário local pelo vínculo.</li>
 * </ul>
 *
 * @since 3.5.0
 */
public class OidcBearerAuthenticator implements ExternalBearerAuthenticator {

    public static final String USER_NOT_LINKED = "USER_NOT_LINKED";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String TOKEN_REVOKED = "TOKEN_REVOKED";
    public static final String IDP_UNAVAILABLE = "IDP_UNAVAILABLE";

    private static final Logger log = LoggerFactory.getLogger(OidcBearerAuthenticator.class);

    private final OidcProviderRegistry registry;
    private final OidcIdentityLinker linker;
    private final OidcRoleSynchronizer roles;
    private final OidcIntrospector introspector;

    public OidcBearerAuthenticator(OidcProviderRegistry registry, OidcIdentityLinker linker,
                                   OidcRoleSynchronizer roles, OidcIntrospector introspector) {
        this.registry = registry;
        this.linker = linker;
        this.roles = roles;
        this.introspector = introspector;
    }

    @Override
    public boolean supports(String token) {
        return UnverifiedJwt.issuer(token).flatMap(registry::match).isPresent();
    }

    @Override
    public ExternalTokenCheck check(String token, HttpServletRequest request) {
        Optional<OidcProviderRegistry.Match> match = UnverifiedJwt.issuer(token).flatMap(registry::match);
        if (match.isEmpty()) {
            return rejected(INVALID_TOKEN, "Token inválido");
        }
        String providerId = match.get().providerId();
        Provider provider = match.get().provider();

        Jwt jwt;
        try {
            jwt = registry.decoder(providerId).decode(token);
        } catch (JwtValidationException e) {
            boolean expired = e.getErrors().stream().map(OAuth2Error::getErrorCode)
                    .anyMatch(OidcProviderRegistry.ERROR_EXPIRED::equals);
            log.info("oidc.token.rejected provider={} errors={}", providerId,
                    e.getErrors().stream().map(OAuth2Error::getErrorCode).toList());
            return expired ? rejected(TOKEN_EXPIRED, "Token expirado") : rejected(INVALID_TOKEN, "Token inválido");
        } catch (JwtException e) {
            // Assinatura, algoritmo, kid desconhecido, JWKS fora do ar: o detalhe é do operador, não do cliente.
            log.warn("oidc.token.invalid provider={} cause={}", providerId, e.getMessage());
            return rejected(INVALID_TOKEN, "Token inválido");
        }

        // Só depois da assinatura: o IdP não é consultado com token forjado.
        if (introspector != null && introspector.enabled(providerId)) {
            OidcIntrospector.Status status = introspector.check(providerId, token, jwt.getExpiresAt());
            if (status == OidcIntrospector.Status.INACTIVE) {
                log.info("oidc.token.revoked provider={}", providerId);
                return rejected(TOKEN_REVOKED, "Token revogado");
            }
            if (status == OidcIntrospector.Status.UNAVAILABLE
                    && provider.getIntrospection().getOnError() == ArchbaseOidcProperties.OnError.DENY) {
                return ExternalTokenCheck.rejected(503, IDP_UNAVAILABLE,
                        "Não foi possível confirmar o token com o provedor de identidade");
            }
        }

        Map<String, Object> claims = jwt.getClaims();
        String subject = ClaimReader.readString(claims, provider.getClaims().getSubject());
        if (subject == null) {
            log.warn("oidc.token.no_subject provider={} claim={}", providerId, provider.getClaims().getSubject());
            return rejected(INVALID_TOKEN, "Token inválido");
        }

        String tenant = tenantOf(match.get(), provider, claims);
        if (tenant == null && !provider.getTenant().isHeader()) {
            log.warn("oidc.token.no_tenant provider={}", providerId);
            return ExternalTokenCheck.rejected(403, "TENANT_REQUIRED", "O token não identifica o tenant");
        }

        String emailClaim = provider.getClaims().getEmailVerified();
        OidcIdentity identity = new OidcIdentity(providerId, subject,
                ClaimReader.readString(claims, provider.getClaims().getEmail()),
                emailClaim != null && !emailClaim.isBlank() && ClaimReader.readTrue(claims, emailClaim),
                ClaimReader.readString(claims, provider.getClaims().getName()),
                tenant, claims);
        // tenant null = o filtro exige e usa o X-TENANT-ID
        return ExternalTokenCheck.accepted(tenant, identity);
    }

    private static String tenantOf(OidcProviderRegistry.Match match, Provider provider, Map<String, Object> claims) {
        if (match.tenantFromIssuer() != null) {
            return match.tenantFromIssuer();
        }
        if (provider.getTenant().getFixed() != null && !provider.getTenant().getFixed().isBlank()) {
            return provider.getTenant().getFixed();
        }
        if (provider.getTenant().getClaim() != null && !provider.getTenant().getClaim().isBlank()) {
            return ClaimReader.readString(claims, provider.getTenant().getClaim());
        }
        return null;
    }

    @Override
    public Authentication authenticate(ExternalTokenCheck check, HttpServletRequest request) {
        OidcIdentity identity = (OidcIdentity) check.claims();
        String tenant = ArchbaseTenantContext.getTenantId();
        UserEntity user = linker.resolve(identity, tenant).orElseThrow(() -> {
            log.info("oidc.user.not_linked provider={} tenant={}", identity.providerId(), tenant);
            return new ExternalAuthenticationRejectedException(403, USER_NOT_LINKED,
                    "Usuário autenticado pelo provedor, mas sem cadastro vinculado nesta aplicação");
        });
        // Antes da checagem de conta ativa: o pendente de aprovação já fica com os grupos certos quando for ativado.
        if (roles != null) {
            user = roles.sync(user, identity);
        }
        if (Boolean.TRUE.equals(user.getAccountDeactivated()) || Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new ExternalAuthenticationRejectedException(403, USER_DISABLED, "Usuário inativo ou bloqueado");
        }
        return new OidcAuthentication(user, identity);
    }

    private static ExternalTokenCheck rejected(String code, String message) {
        return ExternalTokenCheck.rejected(401, code, message);
    }
}
