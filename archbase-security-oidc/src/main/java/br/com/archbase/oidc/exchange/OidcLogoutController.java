package br.com.archbase.oidc.exchange;

import br.com.archbase.oidc.auth.OidcAuthentication;
import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.user.RevokedTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code POST /api/v1/auth/oidc/logout}: encerra, <b>nesta aplicação</b>, a sessão feita com token do IdP.
 * <p>
 * O token do IdP vale pela assinatura até o {@code exp}, e o logout do core ({@code /api/v1/auth/logout}) não o
 * conhece — procura só tokens locais. Aqui o token entra na lista de revogados ({@link RevokedTokenStore}) e passa a
 * ser recusado em todas as instâncias, mesmo que o IdP não o revogue e mesmo sem introspecção.
 * <p>
 * Encerrar a sessão <b>no IdP</b> é do navegador (OIDC RP-Initiated Logout): a resposta traz o
 * {@code end_session_endpoint} configurado, para o frontend redirecionar com o {@code id_token_hint}.
 * <p>
 * Sessão local emitida pela troca de token usa o logout do core, como qualquer sessão local.
 *
 * @since 3.5.0
 */
@RestController
@RequestMapping("/api/v1/auth/oidc")
public class OidcLogoutController {

    private static final Logger log = LoggerFactory.getLogger(OidcLogoutController.class);

    private final RevokedTokenStore revoked;
    private final ArchbaseOidcProperties properties;

    public OidcLogoutController(RevokedTokenStore revoked, ArchbaseOidcProperties properties) {
        this.revoked = revoked;
        this.properties = properties;
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String header = request.getHeader("Authorization");
        if (!(auth instanceof OidcAuthentication oidc) || header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "code", "NOT_AN_OIDC_SESSION",
                    "message", "A requisição não está autenticada por token de provedor OIDC"));
        }
        String providerId = oidc.getIdentity().providerId();
        Object exp = oidc.getIdentity().claims().get("exp");
        revoked.revoke(header.substring(7), providerId, exp instanceof Instant i ? i : null);
        log.info("oidc.logout provider={} userId={}", providerId, oidc.getName());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("revoked", true);
        String endSession = properties.getProviders().get(providerId).getLogout().getEndSessionUri();
        if (endSession != null && !endSession.isBlank()) {
            body.put("end_session_endpoint", endSession);
        }
        return ResponseEntity.ok(body);
    }
}
