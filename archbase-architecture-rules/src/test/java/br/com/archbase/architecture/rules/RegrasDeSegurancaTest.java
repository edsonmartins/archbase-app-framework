package br.com.archbase.architecture.rules;

import br.com.archbase.architecture.rules.spring.ArchbaseSpringRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Regras de segurança dos controllers")
class RegrasDeSegurancaTest {

    private final JavaClasses classes =
            new ClassFileImporter().importPackages("br.com.archbase.architecture.rules.fixtures.seguranca");

    @Test
    @DisplayName("@HasPermission no método e @RequireRole na classe passam; só @PermitAll é acusado (antes, passava)")
    void metodos() {
        var resultado = ArchbaseSpringRules.controllerMethodsShouldHaveSecurityAnnotations().evaluate(classes);
        var detalhes = resultado.getFailureReport().getDetails();
        assertEquals(1, detalhes.size(), detalhes.toString());
        assertTrue(detalhes.get(0).contains("SoPermitAll.aberto"), detalhes.toString());
    }

    @Test
    @DisplayName("segurança na classe ou em todos os métodos passa (antes, só o nome com Public passava)")
    void classes() {
        var resultado = ArchbaseSpringRules.restEndpointsShouldHaveExplicitSecurity().evaluate(classes);
        var detalhes = resultado.getFailureReport().getDetails();
        assertEquals(1, detalhes.size(), detalhes.toString());
        assertTrue(detalhes.get(0).contains("SoPermitAll"), detalhes.toString());
    }
}
