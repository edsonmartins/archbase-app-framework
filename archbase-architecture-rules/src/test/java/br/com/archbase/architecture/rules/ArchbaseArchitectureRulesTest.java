package br.com.archbase.architecture.rules;

import br.com.archbase.architecture.rules.core.ArchbaseArchitectureRules;
import br.com.archbase.architecture.rules.core.ArchbaseRuleProfiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testes para o módulo archbase-architecture-rules.
 */
class ArchbaseArchitectureRulesTest {

    @Test
    @DisplayName("Deve criar instância de ArchbaseArchitectureRules")
    void shouldCreateArchbaseArchitectureRulesInstance() {
        ArchbaseArchitectureRules rules = ArchbaseArchitectureRules.forNamespace("br.com.archbase");
        assertNotNull(rules);
    }

    @Test
    @DisplayName("Deve criar profile multitenantRestApi")
    void shouldCreateMultitenantRestApiProfile() {
        ArchbaseArchitectureRules rules = ArchbaseRuleProfiles.multitenantRestApi("br.com.archbase");
        assertNotNull(rules);
    }

    @Test
    @DisplayName("Deve criar profile simpleService")
    void shouldCreateSimpleServiceProfile() {
        ArchbaseArchitectureRules rules = ArchbaseRuleProfiles.simpleService("br.com.archbase");
        assertNotNull(rules);
    }

    @Test
    @DisplayName("Deve criar profile lenient")
    void shouldCreateLenientProfile() {
        ArchbaseArchitectureRules rules = ArchbaseRuleProfiles.lenient("br.com.archbase");
        assertNotNull(rules);
    }

    @Test
    @DisplayName("Deve permitir encadeamento de métodos")
    void shouldAllowMethodChaining() {
        assertDoesNotThrow(() -> {
            ArchbaseArchitectureRules.forNamespace("br.com.archbase.test")
                    .withDddRules()
                    .withSpringRules()
                    .withNamingRules()
                    .withSecurityRules()
                    .withMultitenancyRules()
                    .failOnEmpty(false)
                    .checkAll(true);
        });
    }
}
