package br.com.archbase.oidc.exchange;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.oidc.auth.OidcBearerAuthenticator;
import br.com.archbase.security.auth.AuthenticationResponse;
import br.com.archbase.security.auth.external.ExternalAuthenticationRejectedException;
import br.com.archbase.security.auth.external.ExternalTokenCheck;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.service.ArchbaseAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code POST /api/v1/auth/oidc/exchange}: troca o access token do IdP por uma sessão local do Archbase.
 * <p>
 * Para frontends que ainda dependem do JWT próprio (refresh, logout e seleção de tenant do archbase-react). O token
 * do IdP vem no <b>corpo</b> ({@code {"token": "..."}}), não no {@code Authorization}, e passa exatamente pela mesma
 * validação e regra de tenant que teria se fosse usado direto na API. A sessão emitida é curta e sem refresh token
 * ({@link ArchbaseAuthenticationService#issueExternalSession}): quando vence, o cliente troca de novo, e é o IdP quem
 * decide se a pessoa continua tendo acesso.
 * <p>
 * Registrado só com {@code archbase.security.oidc.exchange.enabled=true}. Fica sob {@code /api/v1/auth/**}, que já é
 * público.
 *
 * @since 3.5.0
 */
@RestController
@RequestMapping("/api/v1/auth/oidc")
public class OidcTokenExchangeController {

    static final String X_TENANT_ID = "X-TENANT-ID";

    private static final Logger log = LoggerFactory.getLogger(OidcTokenExchangeController.class);

    private final OidcBearerAuthenticator authenticator;
    private final ArchbaseAuthenticationService authenticationService;
    private final Duration ttl;

    public OidcTokenExchangeController(OidcBearerAuthenticator authenticator,
                                       ArchbaseAuthenticationService authenticationService, Duration ttl) {
        this.authenticator = authenticator;
        this.authenticationService = authenticationService;
        this.ttl = ttl;
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody(required = false) Map<String, String> body,
                                      HttpServletRequest request) {
        String token = body == null ? null : body.get("token");
        if (token == null || !authenticator.supports(token)) {
            return error(401, OidcBearerAuthenticator.INVALID_TOKEN, "Token inválido");
        }
        ExternalTokenCheck check;
        try {
            check = authenticator.check(token, request);
        } catch (RuntimeException e) {
            log.error("oidc.exchange.check_failed: {}", e.getMessage(), e);
            return error(401, OidcBearerAuthenticator.INVALID_TOKEN, "Token inválido");
        }
        if (check == null || !check.isAccepted()) {
            return check == null ? error(401, OidcBearerAuthenticator.INVALID_TOKEN, "Token inválido")
                    : error(check.status(), check.code(), check.message());
        }

        // Mesma regra do filtro: o tenant do token vence; divergente é recusado; sem tenant no token, o header manda.
        String header = request.getHeader(X_TENANT_ID);
        String tenant = check.tenantId();
        if (tenant == null || tenant.isBlank()) {
            if (header == null || header.isBlank()) {
                return error(403, "TENANT_REQUIRED", "A requisição não identifica o tenant");
            }
            tenant = header;
        } else if (header != null && !header.isBlank() && !header.equals(tenant)) {
            return error(403, "TENANT_MISMATCH", "X-TENANT-ID não corresponde ao tenant do token");
        }

        String previousTenant = ArchbaseTenantContext.getTenantId();
        try {
            ArchbaseTenantContext.setTenantId(tenant);
            Authentication authentication = authenticator.authenticate(check, request);
            UserEntity user = (UserEntity) authentication.getPrincipal();
            AuthenticationResponse response = authenticationService.issueExternalSession(user, ttl);
            log.info("oidc.exchange.issued tenant={} userId={}", tenant, user.getId());
            return ResponseEntity.ok(response);
        } catch (ExternalAuthenticationRejectedException e) {
            return error(e.getStatus(), e.getCode(), e.getMessage());
        } catch (BadCredentialsException e) {
            return error(403, OidcBearerAuthenticator.USER_DISABLED, "Usuário inativo ou bloqueado");
        } finally {
            if (previousTenant == null) {
                ArchbaseTenantContext.clear();
            } else {
                ArchbaseTenantContext.setTenantId(previousTenant);
            }
        }
    }

    private static ResponseEntity<Map<String, Object>> error(int status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", code);
        body.put("code", code);
        body.put("message", message);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (status == 401) {
            builder.header("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        }
        return builder.body(body);
    }
}
