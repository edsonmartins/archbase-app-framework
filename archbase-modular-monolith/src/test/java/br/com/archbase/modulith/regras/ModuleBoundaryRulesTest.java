package br.com.archbase.modulith.regras;

import br.com.archbase.modulith.rules.ModuleBoundaryRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As regras examinam classes de verdade. Até a 3.5.0 o ArchUnit 1.0.1 não lia o bytecode do Java 25 (major 69):
 * importava zero classes, e dois defeitos destas regras nunca apareceram.
 */
@DisplayName("ModuleBoundaryRules")
class ModuleBoundaryRulesTest {

    private static final String BASE = "br.com.archbase.modulith.regras.exemplo";
    private final JavaClasses classes = new ClassFileImporter().importPackages(BASE);

    @Test
    @DisplayName("o ArchUnit lê as classes do Java 25")
    void importa() {
        assertThat(classes).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("classe só com @Module é aceita (o @Component vem da meta-anotação)")
    void moduloSoComAnotacao() {
        assertThat(ModuleBoundaryRules.moduleClassesShouldBeConfigurations().evaluate(classes).hasViolation()).isFalse();
    }

    @Test
    @DisplayName("interno do próprio módulo é permitido; interno de outro módulo é acusado")
    void internoDeOutroModulo() {
        var resultado = ModuleBoundaryRules.modulesShouldNotAccessOtherModulesInternals(BASE).evaluate(classes);

        assertThat(resultado.hasViolation()).isTrue();
        assertThat(resultado.getFailureReport().getDetails())
                .allMatch(d -> d.contains("Fatura") && d.contains("estoque.internal.Reserva"));
    }
}
