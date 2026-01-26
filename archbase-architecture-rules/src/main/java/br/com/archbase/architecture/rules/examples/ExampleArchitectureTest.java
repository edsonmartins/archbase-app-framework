package br.com.archbase.architecture.rules.examples;

import br.com.archbase.architecture.rules.core.ArchbaseArchitectureRules;
import br.com.archbase.architecture.rules.core.ArchbaseRuleProfiles;
import br.com.archbase.architecture.rules.ddd.ArchbaseDddRules;
import br.com.archbase.architecture.rules.test.ArchbaseArchitectureTest;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exemplos de como usar as regras de arquitetura Archbase em projetos.
 * <p>
 * ATENÇÃO: Esta classe contém apenas exemplos de código.
 * Copie e adapte para seu projeto conforme necessário.
 *
 * @author Archbase Team
 * @since 2.0.1
 */
@Disabled("Esta classe contém apenas exemplos - não executar como teste")
public class ExampleArchitectureTest {

    // =====================================================================
    // EXEMPLO 1: Uso básico com API fluente
    // =====================================================================

    @Test
    @DisplayName("Exemplo: Validar padrões básicos do projeto")
    void example1_basicUsage() {
        ArchbaseArchitectureRules.forNamespace("com.minhaempresa.meuprojeto")
                .withDddRules()       // Entidades, repositórios, separação de camadas
                .withSpringRules()    // Controllers, Services, @Autowired
                .withNamingRules()    // Convenções de nomes
                .check();
    }

    // =====================================================================
    // EXEMPLO 2: API REST com multitenancy
    // =====================================================================

    @Test
    @DisplayName("Exemplo: Validar API REST multitenancy")
    void example2_multitenantRestApi() {
        ArchbaseArchitectureRules.forNamespace("com.minhaempresa.meuprojeto")
                .withDddRules()
                .withSpringRules()
                .withNamingRules()
                .withSecurityRules()       // @HasPermission em controllers
                .withMultitenancyRules()   // TenantPersistenceEntityBase
                .checkAll(true)            // Mostra todos os erros de uma vez
                .check();
    }

    // =====================================================================
    // EXEMPLO 3: Usando profiles predefinidos
    // =====================================================================

    @Test
    @DisplayName("Exemplo: Usar profile para API multitenancy")
    void example3_usingProfiles() {
        // Profile completo para API REST com multitenancy
        ArchbaseRuleProfiles.multitenantRestApi("com.minhaempresa.meuprojeto").check();
    }

    @Test
    @DisplayName("Exemplo: Usar profile para microservice simples")
    void example3b_simpleService() {
        // Profile para microservice sem segurança complexa
        ArchbaseRuleProfiles.simpleService("com.minhaempresa.meuprojeto").check();
    }

    @Test
    @DisplayName("Exemplo: Usar profile leniente para migração")
    void example3c_lenientProfile() {
        // Profile com regras mínimas para projetos em migração
        ArchbaseRuleProfiles.lenient("com.minhaempresa.meuprojeto").check();
    }

    // =====================================================================
    // EXEMPLO 4: Estendendo classe base
    // =====================================================================

    /**
     * Exemplo de teste estendendo ArchbaseArchitectureTest.
     * Copie esta classe interna para seu projeto como classe separada.
     */
    static class MeuProjetoArchitectureTest extends ArchbaseArchitectureTest {

        @Override
        protected String getBasePackage() {
            return "com.minhaempresa.meuprojeto";
        }

        @Override
        protected boolean enableSecurityRules() {
            return true; // Habilita validação de @HasPermission
        }

        @Override
        protected boolean enableMultitenancyRules() {
            return true; // Habilita validação de TenantPersistenceEntityBase
        }

        @Override
        protected void configureCustomRules(ArchbaseArchitectureRules rules) {
            // Adiciona regras específicas do projeto
            rules.addRule(TaikaiRule.of(
                    ArchRuleDefinition.classes()
                            .that().resideInAPackage("..api..")
                            .should().haveSimpleNameEndingWith("Api")
            ));
        }
    }

    // =====================================================================
    // EXEMPLO 5: Regras customizadas
    // =====================================================================

    @Test
    @DisplayName("Exemplo: Adicionar regras customizadas")
    void example5_customRules() {
        ArchbaseArchitectureRules.forNamespace("com.minhaempresa.meuprojeto")
                .withDddRules()
                .withSpringRules()
                // Regra customizada: handlers devem terminar com Handler
                .addRule(TaikaiRule.of(
                        ArchRuleDefinition.classes()
                                .that().resideInAPackage("..handler..")
                                .should().haveSimpleNameEndingWith("Handler")
                ))
                // Regra customizada: use cases devem terminar com UseCase
                .addRule(TaikaiRule.of(
                        ArchRuleDefinition.classes()
                                .that().resideInAPackage("..usecase..")
                                .should().haveSimpleNameEndingWith("UseCase")
                ))
                .check();
    }

    // =====================================================================
    // EXEMPLO 6: Validar regra de repositórios sem métodos customizados
    // =====================================================================

    @Test
    @DisplayName("Exemplo: Validar que repositórios não têm métodos customizados")
    void example6_repositoriesWithoutCustomMethods() {
        // Esta regra garante que todas as queries sejam feitas via QueryDSL no adapter
        ArchbaseDddRules.repositoriesShouldNotHaveCustomMethods()
                .check(new com.tngtech.archunit.core.importer.ClassFileImporter()
                        .importPackages("com.minhaempresa.meuprojeto"));
    }

    // =====================================================================
    // EXEMPLO 7: Integração com CI/CD
    // =====================================================================

    /**
     * Para integrar com CI/CD, crie um teste simples que será executado
     * automaticamente durante o build:
     *
     * <pre>{@code
     * // src/test/java/com/minhaempresa/ArchitectureTest.java
     *
     * package com.minhaempresa;
     *
     * import br.com.archbase.architecture.rules.core.ArchbaseRuleProfiles;
     * import org.junit.jupiter.api.Test;
     *
     * class ArchitectureTest {
     *
     *     @Test
     *     void shouldFollowArchbasePatterns() {
     *         ArchbaseRuleProfiles.multitenantRestApi("com.minhaempresa").check();
     *     }
     * }
     * }</pre>
     *
     * O teste falhará se alguma regra for violada, bloqueando o build.
     */
    @Test
    void example7_ciCdIntegration() {
        // Este é o teste que rodará no CI/CD
        ArchbaseRuleProfiles.multitenantRestApi("com.minhaempresa.meuprojeto").check();
    }
}
