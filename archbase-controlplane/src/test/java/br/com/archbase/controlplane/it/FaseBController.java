package br.com.archbase.controlplane.it;

import br.com.archbase.controlplane.ControlPlane;
import br.com.archbase.controlplane.access.ExportOperation;
import br.com.archbase.controlplane.access.RequiresEntitlement;
import br.com.archbase.controlplane.access.RequiresSeat;
import br.com.archbase.controlplane.api.Usage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teste/b")
public class FaseBController {

    @PostMapping("/escrita")
    public Map<String, Object> escrita() {
        return Map.of("ok", true);
    }

    @PostMapping("/exportar")
    @ExportOperation
    public Map<String, Object> exportar() {
        return Map.of("ok", true);
    }

    @GetMapping("/excel")
    @RequiresEntitlement("powerview.export_excel")
    public Map<String, Object> excel() {
        return Map.of("ok", true);
    }

    @GetMapping("/bi")
    @RequiresEntitlement("powerview.bi")
    public Map<String, Object> bi() {
        return Map.of("ok", true);
    }

    @GetMapping("/com-assento")
    @RequiresSeat
    public Map<String, Object> comAssento() {
        return Map.of("ok", true);
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantState", ControlPlane.tenantState().name());
        body.put("license", ControlPlane.license().status().name());
        body.put("excel", ControlPlane.entitlements().can("powerview.export_excel"));
        body.put("assentos", ControlPlane.entitlements().value("powerview.assentos"));
        body.put("seat", ControlPlane.seats().check("cp-ana"));
        return body;
    }

    @PostMapping("/consumir")
    @ExportOperation
    public Usage consumir(@RequestParam long qtd) {
        return ControlPlane.entitlements().consume("copilot.ia_creditos", qtd);
    }
}
