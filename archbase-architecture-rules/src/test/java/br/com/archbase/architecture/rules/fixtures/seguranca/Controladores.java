package br.com.archbase.architecture.rules.fixtures.seguranca;

import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.annotations.RequireRole;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.RestController;

/** Fixtures das regras de segurança dos controllers. */
public final class Controladores {

    private Controladores() {
    }

    @RestController
    public static class PorMetodo {
        @HasPermission(action = "VIEW", resource = "PEDIDO", description = "Ver pedido")
        public String ver() {
            return "";
        }
    }

    @RestController
    @RequireRole("GERENTE")
    public static class PelaClasse {
        public String listar() {
            return "";
        }
    }

    @RestController
    public static class SoPermitAll {
        @PermitAll
        public String aberto() {
            return "";
        }
    }
}
