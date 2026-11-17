package br.com.archbase.controlplane.it;

import br.com.archbase.controlplane.access.AdminScope;
import br.com.archbase.controlplane.access.RequiresAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** {@code @RequiresAdmin} na classe, com um método que a sobrescreve. */
@RestController
@RequestMapping("/api/teste/classe-cliente")
@RequiresAdmin(AdminScope.CLIENT)
public class ClientAdminController {

    @GetMapping("/padrao")
    public Map<String, Object> padrao() {
        return Map.of("ok", true);
    }

    @GetMapping("/de-tenant")
    @RequiresAdmin(AdminScope.TENANT)
    public Map<String, Object> deTenant() {
        return Map.of("ok", true);
    }
}
