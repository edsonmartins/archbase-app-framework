package br.com.archbase.oidc.migration;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.migration.IdentityMigrationService.Request;
import br.com.archbase.security.auth.ArchbaseCurrentUser;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Migração de identidades para um provedor OIDC, no tenant da requisição ({@link IdentityMigrationService}).
 * <ul>
 * <li>{@code GET  /export?providerId=} — usuários do tenant, para importar no IdP de destino;</li>
 * <li>{@code POST /plan} — dry-run; devolve o relatório e o {@code planHash};</li>
 * <li>{@code POST /execute} — grava, se o {@code planHash} ainda for o do relatório ({@code 409 PLAN_CHANGED}
 * com o plano novo, se não for);</li>
 * <li>{@code GET  /history} — execuções e dry-runs do tenant.</li>
 * </ul>
 * <b>Exige {@code isAdministrator}</b>, verificado aqui, como na API de vínculo.
 *
 * @since 3.5.0
 */
@RestController
@RequestMapping("/api/v1/security/identity-migration")
public class IdentityMigrationController {

    private final IdentityMigrationService service;
    private final IdentityMigrationJournal journal;
    private final ArchbaseOidcProperties properties;

    public IdentityMigrationController(IdentityMigrationService service, IdentityMigrationJournal journal,
                                       ArchbaseOidcProperties properties) {
        this.service = service;
        this.journal = journal;
        this.properties = properties;
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam String providerId) {
        Optional<UserEntity> admin = administrator();
        if (admin.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ResponseEntity<?> invalid = validate(providerId);
        if (invalid != null) {
            return invalid;
        }
        return ResponseEntity.ok(service.export(providerId, ArchbaseTenantContext.getTenantId()));
    }

    @PostMapping("/plan")
    public ResponseEntity<?> plan(@RequestBody Request request) {
        Optional<UserEntity> admin = administrator();
        if (admin.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ResponseEntity<?> invalid = validate(request);
        if (invalid != null) {
            return invalid;
        }
        return ResponseEntity.ok(service.plan(request, ArchbaseTenantContext.getTenantId(), admin.get().getId()));
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody Request request) {
        Optional<UserEntity> admin = administrator();
        if (admin.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ResponseEntity<?> invalid = validate(request);
        if (invalid != null) {
            return invalid;
        }
        if (request.planHash() == null || request.planHash().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "PLAN_REQUIRED",
                    "Rode o dry-run (/plan) e envie o planHash do relatório conferido");
        }
        try {
            return ResponseEntity.ok(service.execute(request, ArchbaseTenantContext.getTenantId(),
                    admin.get().getId()));
        } catch (IdentityMigrationService.PlanChangedException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", 409);
            body.put("code", "PLAN_CHANGED");
            body.put("message", "O resultado mudou desde o dry-run; confira o plano novo e execute com o hash dele");
            body.put("plan", e.current());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> history() {
        if (administrator().isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(journal.findByTenant(ArchbaseTenantContext.getTenantId()));
    }

    private ResponseEntity<?> validate(Request request) {
        if (request == null) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Corpo obrigatório");
        }
        ResponseEntity<?> invalid = validate(request.providerId());
        if (invalid != null) {
            return invalid;
        }
        if (request.entries() == null || request.entries().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "entries é obrigatório");
        }
        if (request.entries().size() > IdentityMigrationService.MAX_ENTRIES) {
            return error(HttpStatus.BAD_REQUEST, "TOO_MANY_ENTRIES",
                    "No máximo " + IdentityMigrationService.MAX_ENTRIES + " linhas por plano; divida o arquivo");
        }
        return null;
    }

    private ResponseEntity<?> validate(String providerId) {
        if (ArchbaseTenantContext.getTenantId() == null) {
            return error(HttpStatus.BAD_REQUEST, "TENANT_REQUIRED", "A migração é por tenant");
        }
        if (providerId == null || !properties.getProviders().containsKey(providerId)) {
            return error(HttpStatus.BAD_REQUEST, "UNKNOWN_PROVIDER", "Provedor não configurado: " + providerId);
        }
        return null;
    }

    private static Optional<UserEntity> administrator() {
        return ArchbaseCurrentUser.of(SecurityContextHolder.getContext().getAuthentication())
                .filter(u -> Boolean.TRUE.equals(u.getIsAdministrator()) && u.isEnabled());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "code", code, "message", message));
    }
}
