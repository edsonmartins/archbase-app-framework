package br.com.archbase.architecture.rules;

import br.com.archbase.architecture.rules.architecture.ArchbaseArchitectureStyleRules;
import br.com.archbase.architecture.rules.ddd.ArchbaseAggregateRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada cenário tem casos válidos e violações conhecidas nos fixtures; o teste confere a lista exata de origens
 * acusadas, para pegar tanto falso negativo (regra inerte) quanto falso positivo.
 */
class ArchitectureStyleAndAggregateRulesTest {

    private static final String FIXTURES = "br.com.archbase.architecture.rules.fixtures.";

    private static List<String> violations(ArchRule rule, String pkg) {
        JavaClasses classes = new ClassFileImporter().importPackages(FIXTURES + pkg);
        return rule.evaluate(classes).getFailureReport().getDetails();
    }

    private static List<String> violatingOrigins(ArchRule rule, String pkg) {
        return violations(rule, pkg).stream()
                .map(detail -> detail.substring(0, detail.indexOf(' ')))
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .distinct()
                .sorted()
                .toList();
    }

    @Nested
    @DisplayName("Camadas")
    class Layered {

        @Test
        @DisplayName("relaxado: só acusa dependência para camada superior")
        void relaxed() {
            assertEquals(List.of("DomainCallingUp"), violatingOrigins(ArchbaseArchitectureStyleRules.layered(), "layered"));
        }

        @Test
        @DisplayName("estrito: também acusa quem pula camada")
        void strict() {
            assertEquals(List.of("DomainCallingUp", "UiController"), violatingOrigins(ArchbaseArchitectureStyleRules.layeredStrict(), "layered"));
        }

        @Test
        @DisplayName("mensagem identifica as duas camadas")
        void message() {
            assertTrue(violations(ArchbaseArchitectureStyleRules.layered(), "layered").stream()
                    .anyMatch(d -> d.contains("@DomainLayer") && d.contains("@ApplicationLayer")));
        }
    }

    @Nested
    @DisplayName("Onion")
    class Onion {

        @Test
        @DisplayName("clássica: anel interno não depende de anel externo")
        void classical() {
            assertEquals(List.of("Pedido"), violatingOrigins(ArchbaseArchitectureStyleRules.onionClassical(), "onion"));
        }

        @Test
        @DisplayName("regra de outro estilo ignora pacotes sem as suas anotações")
        void otherStyleIgnoresUnannotated() {
            assertTrue(violations(ArchbaseArchitectureStyleRules.onionSimplified(), "onion").isEmpty());
            assertTrue(violations(ArchbaseArchitectureStyleRules.layered(), "onion").isEmpty());
            assertTrue(violations(ArchbaseArchitectureStyleRules.hexagonal(), "layered").isEmpty());
        }
    }

    @Nested
    @DisplayName("Hexagonal")
    class Hexagonal {

        @Test
        @DisplayName("núcleo não depende de adaptador; primário não depende de secundário")
        void hexagonal() {
            assertEquals(List.of("AtalhoProibido", "ControllerQueVaiAoBanco"), violatingOrigins(ArchbaseArchitectureStyleRules.hexagonal(), "hexagonal"));
        }
    }

    @Nested
    @DisplayName("Agregados")
    class Aggregates {

        @Test
        @DisplayName("entidade só aparece no agregado dono (resolve genérico por classe base)")
        void entitiesInOwningAggregate() {
            List<String> details = violations(
                    ArchbaseAggregateRules.entitiesShouldBeDeclaredForUseInSameAggregate(), "aggregates");

            assertEquals(2, details.size(), details.toString());
            assertTrue(details.stream().anyMatch(d -> d.contains("Order.shippingAddress")), details.toString());
            assertTrue(details.stream().anyMatch(d -> d.contains("Order.addressesByType")), details.toString());
        }

        @Test
        @DisplayName("agregado referenciado só por id ou associação (tipo, anotação, Optional e array)")
        void referencesViaId() {
            List<String> details = violations(
                    ArchbaseAggregateRules.aggregateReferencesShouldBeViaIdOrAssociation(), "aggregates");

            assertEquals(3, details.size(), details.toString());
            assertTrue(details.stream().anyMatch(d -> d.contains("Order.customer ")), details.toString());
            assertTrue(details.stream().anyMatch(d -> d.contains("Order.maybeCustomer")), details.toString());
            assertTrue(details.stream().anyMatch(d -> d.contains("Order.invoices")), details.toString());
            assertTrue(details.stream().noneMatch(d -> d.contains("OrderDto")), details.toString());
            assertTrue(details.stream().noneMatch(d -> d.contains("CACHE_ESTATICO")), details.toString());
        }

        @Test
        @DisplayName("regras não se aplicam a pacote sem tipos de domínio")
        void noDomainTypes() {
            assertTrue(violations(ArchbaseAggregateRules.all(), "layered").isEmpty());
        }
    }
}
