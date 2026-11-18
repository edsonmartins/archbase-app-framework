package br.com.archbase.controlplane.auth;

import br.com.archbase.controlplane.config.ControlPlaneProperties;
import br.com.archbase.controlplane.session.ControlPlaneAuthentication;
import br.com.archbase.controlplane.session.TenantSession;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import br.com.archbase.controlplane.tenant.TenantState;
import br.com.archbase.controlplane.token.ControlPlaneClaims;
import br.com.archbase.controlplane.token.ControlPlaneTokenValidator;
import br.com.archbase.controlplane.token.TokenValidationResult;
import br.com.archbase.controlplane.user.ControlPlaneUserResolver;
import br.com.archbase.security.auth.external.ExternalAuthenticationPolicy;
import br.com.archbase.security.auth.external.ExternalAuthenticationRejectedException;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import br.com.archbase.security.auth.external.ExternalTokenCheck;
import br.com.archbase.security.persistence.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Liga o token do Control Plane ao {@code ArchbaseJwtAuthenticationFilter}, pelo ponto de extensão
 * {@link ExternalBearerAuthenticator}, e impõe a política do módulo ({@link ExternalAuthenticationPolicy}):
 * tenant padrão nunca, e JWT próprio do Archbase só com {@code legacy-jwt=accept}.
 *
 * @since 3.4.6
 */
public class ControlPlaneBearerAuthenticator implements ExternalBearerAuthenticator, ExternalAuthenticationPolicy {

    /** Authority de quem chega com token de instância. */
    public static final String INSTANCE_AUTHORITY = "ROLE_CP_INSTANCE";

    /** Usuário local desativado ou bloqueado no produto. */
    public static final String USER_DISABLED = "USER_DISABLED";

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneBearerAuthenticator.class);

    private final ControlPlaneTokenValidator validator;
    private final ControlPlaneUserResolver userResolver;
    private final ControlPlaneProperties properties;
    private final ControlPlaneState state;

    public ControlPlaneBearerAuthenticator(ControlPlaneTokenValidator validator, ControlPlaneUserResolver userResolver,
                                           ControlPlaneProperties properties, ControlPlaneState state) {
        this.validator = validator;
        this.userResolver = userResolver;
        this.properties = properties;
        this.state = state;
    }

    @Override
    public boolean supports(String token) {
        return validator.looksLikeControlPlaneToken(token);
    }

    @Override
    public ExternalTokenCheck check(String token, HttpServletRequest request) {
        TokenValidationResult result = validator.validate(token);
        if (!result.isValid()) {
            return ExternalTokenCheck.rejected(result.status(), result.code(), result.message());
        }
        return ExternalTokenCheck.accepted(result.claims().tenantId(), result.claims());
    }

    @Override
    public Authentication authenticate(ExternalTokenCheck check, HttpServletRequest request) {
        ControlPlaneClaims claims = (ControlPlaneClaims) check.claims();
        String tenantId = br.com.archbase.ddd.context.ArchbaseTenantContext.getTenantId();

        if (claims.use() == ControlPlaneClaims.TokenUse.INSTANCE) {
            TenantSession session = new TenantSession(claims.subject(), null, tenantId, null, null, null, true,
                    claims.tokenId(), claims.expiresAt());
            return new ControlPlaneAuthentication(session, session,
                    List.of(new SimpleGrantedAuthority(INSTANCE_AUTHORITY)));
        }

        rejectNewLoginIfSuspended(claims, tenantId);

        Optional<UserEntity> localUser = userResolver.resolve(claims, tenantId);
        if (localUser.isPresent() && (!localUser.get().isEnabled() || !localUser.get().isAccountNonLocked())) {
            log.warn("controlplane.auth.rejected code={} tnt={} sub={}", USER_DISABLED, tenantId, claims.subject());
            throw new ExternalAuthenticationRejectedException(403, USER_DISABLED,
                    "Usuário desativado ou bloqueado neste produto");
        }

        TenantSession session = new TenantSession(claims.subject(), claims.email(), tenantId, claims.adminRole(),
                claims.seatId(), localUser.map(UserEntity::getId).orElse(null), false, claims.tokenId(),
                claims.expiresAt());

        List<GrantedAuthority> authorities = new ArrayList<>();
        localUser.ifPresent(user -> authorities.addAll(user.getAuthorities()));
        authorities.add(new SimpleGrantedAuthority(claims.adminRole().authority()));

        if (localUser.isEmpty()) {
            log.debug("controlplane.auth.no_local_user tnt={} sub={} — @HasPermission vai negar", tenantId,
                    claims.subject());
        }
        return new ControlPlaneAuthentication(localUser.<Object>map(u -> u).orElse(session), session, authorities);
    }

    /**
     * Tenant suspenso recusa login novo: token emitido depois da suspensão (ou sem {@code iat}). A sessão que já
     * existia continua, em modo restrito — leitura e exportação.
     */
    private void rejectNewLoginIfSuspended(ControlPlaneClaims claims, String tenantId) {
        Optional<TenantSnapshot> snapshot = state.tenant(tenantId);
        if (snapshot.isEmpty() || snapshot.get().state() != TenantState.SUSPENDED) {
            return;
        }
        Instant suspendedAt = snapshot.get().suspendedAt();
        boolean newLogin = claims.issuedAt() == null || suspendedAt == null || claims.issuedAt().isAfter(suspendedAt);
        if (!newLogin) {
            return;
        }
        if (properties.getRestrictedMode().getEnforcement() == ControlPlaneProperties.Enforcement.REPORT) {
            log.warn("controlplane.auth.would_deny code=TENANT_SUSPENDED tnt={} sub={} (enforcement=report)",
                    tenantId, claims.subject());
            return;
        }
        log.info("controlplane.auth.rejected code=TENANT_SUSPENDED tnt={} sub={}", tenantId, claims.subject());
        throw new ExternalAuthenticationRejectedException(403, "TENANT_SUSPENDED",
                "Tenant suspenso: não é possível iniciar nova sessão");
    }

    @Override
    public void onRejected(String code, HttpServletRequest request) {
        state.metrics().tokenRejected(code);
    }

    @Override
    public boolean acceptLegacyJwt() {
        return properties.getLegacyJwt() == ControlPlaneProperties.LegacyJwtMode.ACCEPT;
    }

    @Override
    public boolean allowDefaultTenant() {
        return false;
    }
}
