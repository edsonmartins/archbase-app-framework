package br.com.archbase.oidc.exchange;

import br.com.archbase.oidc.config.ArchbaseOidcProperties.Backchannel;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import br.com.archbase.oidc.provider.OidcProviderRegistry;
import br.com.archbase.oidc.token.UnverifiedJwt;
import br.com.archbase.oidc.user.ExternalIdentityStore;
import br.com.archbase.oidc.user.ExternalLogoutStore;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code POST /api/v1/auth/oidc/backchannel-logout}: recebe o aviso de logout do IdP (OIDC Back-Channel Logout 1.0).
 * <p>
 * O IdP chama este endereço servidor a servidor, sem credencial nossa — o que autentica o aviso é a assinatura do
 * {@code logout_token}, verificada com as mesmas chaves dos access tokens do provedor. O provedor é descoberto pelo
 * {@code iss}, como no filtro; provedor sem {@code logout.backchannel.enabled} é recusado.
 * <p>
 * Efeito: registra o corte ({@link ExternalLogoutStore}) — tokens do IdP daquele usuário/sessão emitidos até o aviso
 * passam a ser recusados com {@code 401 TOKEN_REVOKED} — e, com {@code revoke-local-sessions}, revoga as sessões
 * locais dos usuários vinculados (as da troca de token, que não carregam nada do IdP).
 * <p>
 * Respostas da especificação (§2.8): 200 sem corpo; 400 com {@code error}/{@code error_description}; sempre
 * {@code Cache-Control: no-store}. Repetir o mesmo aviso é inofensivo.
 *
 * @since 3.5.0
 */
@RestController
@RequestMapping("/api/v1/auth/oidc")
public class OidcBackchannelLogoutController {

    private static final Logger log = LoggerFactory.getLogger(OidcBackchannelLogoutController.class);

    private final OidcProviderRegistry registry;
    private final ExternalLogoutStore logouts;
    private final ExternalIdentityStore links;
    private final AccessTokenJpaRepository localTokens;
    private final TransactionTemplate transaction;

    public OidcBackchannelLogoutController(OidcProviderRegistry registry, ExternalLogoutStore logouts,
                                           ExternalIdentityStore links, AccessTokenJpaRepository localTokens,
                                           TransactionTemplate transaction) {
        this.registry = registry;
        this.logouts = logouts;
        this.links = links;
        this.localTokens = localTokens;
        this.transaction = transaction;
    }

    @PostMapping(value = "/backchannel-logout", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> backchannelLogout(@RequestParam(name = "logout_token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return error("invalid_request", "logout_token ausente");
        }
        Optional<OidcProviderRegistry.Match> match = UnverifiedJwt.issuer(token).flatMap(registry::match);
        if (match.isEmpty()) {
            return error("invalid_request", "logout_token inválido");
        }
        String providerId = match.get().providerId();
        Provider provider = match.get().provider();
        Backchannel config = provider.getLogout().getBackchannel();
        if (!config.isEnabled()) {
            log.info("oidc.backchannel.disabled provider={}", providerId);
            return error("invalid_request", "back-channel logout não habilitado para este provedor");
        }

        Jwt jwt;
        try {
            jwt = registry.logoutDecoder(providerId).decode(token);
        } catch (JwtValidationException e) {
            log.info("oidc.backchannel.rejected provider={} errors={}", providerId,
                    e.getErrors().stream().map(OAuth2Error::getDescription).toList());
            return error("invalid_request", "logout_token inválido");
        } catch (JwtException e) {
            log.warn("oidc.backchannel.invalid provider={} cause={}", providerId, e.getMessage());
            return error("invalid_request", "logout_token inválido");
        }

        String subject = blankToNull(jwt.getSubject());
        String sid = blankToNull(jwt.getClaimAsString("sid"));
        logouts.record(providerId, subject, sid, jwt.getIssuedAt(), config.getTokenLifetime());

        int local = 0;
        if (subject != null && config.isRevokeLocalSessions() && "sub".equals(provider.getClaims().getSubject())) {
            List<ExternalIdentityStore.Link> linked = links.findBySubject(providerId, subject);
            // Nativa por id do usuário: não depende do tenant no contexto (o aviso não diz o tenant).
            Integer n = transaction.execute(status -> linked.stream()
                    .mapToInt(l -> localTokens.revokeAllTokensOfUser(l.userId())).sum());
            local = n == null ? 0 : n;
        }
        log.info("oidc.backchannel.logout provider={} bySid={} bySub={} localSessionsRevoked={}", providerId,
                sid != null, subject != null, local);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();
    }

    private static ResponseEntity<?> error(String code, String description) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("error", code, "error_description", description));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
