package br.com.archbase.controlplane.it;

import br.com.archbase.controlplane.ControlPlane;
import br.com.archbase.controlplane.access.AdminScope;
import br.com.archbase.controlplane.access.RequiresAdmin;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class TestController {

    /** Rota autenticada: devolve quem está na requisição e o tenant do contexto. */
    @GetMapping("/api/teste/quem")
    public Map<String, Object> quem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", auth.getName());
        body.put("tenant", ArchbaseTenantContext.getTenantId());
        body.put("localUser", auth.getPrincipal() instanceof UserEntity user ? user.getId() : null);
        ControlPlane.findCurrentSession().ifPresent(session -> {
            body.put("sub", session.userId());
            body.put("adm", session.adminRole() == null ? null : session.adminRole().claimValue());
            body.put("seat", session.seatId());
            body.put("instance", session.instance());
        });
        return body;
    }

    /** Rota pública: mostra o tenant que ficou no contexto sem token nenhum. */
    @GetMapping("/api/publico/tenant")
    public Map<String, Object> tenantPublico() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant", ArchbaseTenantContext.getTenantId());
        return body;
    }

    @GetMapping("/api/teste/admin-tenant")
    @RequiresAdmin
    public Map<String, Object> adminTenant() {
        return Map.of("ok", true);
    }

    @GetMapping("/api/teste/admin-cliente")
    @RequiresAdmin(AdminScope.CLIENT)
    public Map<String, Object> adminCliente() {
        return Map.of("ok", true);
    }
}
