package br.com.archbase.oidc.admin;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.user.ExternalIdentityStore;
import br.com.archbase.security.auth.ArchbaseCurrentUser;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Vínculo manual entre identidade do IdP e usuário local, no tenant da requisição — para quem o vínculo automático
 * não alcança: IdP sem e-mail verificado, {@code link-by-email=never}, tenant pelo header, ou migração.
 * <p>
 * <b>Exige {@code isAdministrator}</b>, verificado aqui, explicitamente — não depende de
 * {@code archbase.security.admin-endpoints.policy}, cujo padrão {@code permit} deixaria qualquer autenticado vincular
 * a própria identidade externa à conta de outra pessoa.
 *
 * @since 3.5.0
 */
@RestController
@RequestMapping("/api/v1/security/external-identities")
public class ExternalIdentityAdminController {

    private static final Logger log = LoggerFactory.getLogger(ExternalIdentityAdminController.class);

    private final ExternalIdentityStore store;
    private final UserJpaRepository users;
    private final ArchbaseOidcProperties properties;

    public ExternalIdentityAdminController(ExternalIdentityStore store, UserJpaRepository users,
                                           ArchbaseOidcProperties properties) {
        this.store = store;
        this.users = users;
        this.properties = properties;
    }

    public record LinkRequest(String providerId, String subject, String userId) {
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam String userId) {
        Optional<UserEntity> admin = administrator();
        if (admin.isEmpty()) {
            return forbidden();
        }
        return ResponseEntity.ok(store.findByUser(ArchbaseTenantContext.getTenantId(), userId));
    }

    @PostMapping
    public ResponseEntity<?> link(@RequestBody LinkRequest request) {
        Optional<UserEntity> admin = administrator();
        if (admin.isEmpty()) {
            return forbidden();
        }
        String tenant = ArchbaseTenantContext.getTenantId();
        if (request == null || blank(request.providerId()) || blank(request.subject()) || blank(request.userId())) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "providerId, subject e userId são obrigatórios");
        }
        if (!properties.getProviders().containsKey(request.providerId())) {
            return error(HttpStatus.BAD_REQUEST, "UNKNOWN_PROVIDER", "Provedor não configurado: " + request.providerId());
        }
        // O usuário precisa existir no tenant da requisição: consulta JPQL, filtrada pelo @TenantId.
        if (users.findByIdWithGroupsAndProfile(request.userId()).isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado neste tenant");
        }
        Optional<String> current = store.findUserId(request.providerId(), tenant, request.subject());
        if (current.isPresent()) {
            return current.get().equals(request.userId())
                    ? ResponseEntity.ok(Map.of("linked", true))
                    : error(HttpStatus.CONFLICT, "IDENTITY_ALREADY_LINKED",
                    "Esta identidade já está vinculada a outro usuário; desvincule antes");
        }
        Optional<String> otherSubject = store.findSubject(request.providerId(), tenant, request.userId());
        if (otherSubject.isPresent()) {
            return error(HttpStatus.CONFLICT, "USER_ALREADY_LINKED",
                    "O usuário já está vinculado a outra identidade deste provedor; desvincule antes");
        }
        store.link(request.providerId(), tenant, request.subject(), request.userId());
        log.info("oidc.admin.linked by={} provider={} tenant={} userId={}", admin.get().getId(),
                request.providerId(), tenant, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("linked", true));
    }

    @DeleteMapping("/{providerId}/{subject}")
    public ResponseEntity<?> unlink(@PathVariable String providerId, @PathVariable String subject) {
        Optional<UserEntity> admin = administrator();
        if (admin.isEmpty()) {
            return forbidden();
        }
        String tenant = ArchbaseTenantContext.getTenantId();
        if (!store.unlink(providerId, tenant, subject)) {
            return ResponseEntity.notFound().build();
        }
        log.info("oidc.admin.unlinked by={} provider={} tenant={}", admin.get().getId(), providerId, tenant);
        return ResponseEntity.noContent().build();
    }

    private static Optional<UserEntity> administrator() {
        return ArchbaseCurrentUser.of(SecurityContextHolder.getContext().getAuthentication())
                .filter(u -> Boolean.TRUE.equals(u.getIsAdministrator()) && u.isEnabled());
    }

    private static ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "code", code, "message", message));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
