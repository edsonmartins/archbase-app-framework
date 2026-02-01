package br.com.archbase.modulith.rules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import java.util.*;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras ArchUnit para validação de dependências entre módulos.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public final class ModuleDependencyRules {

    private ModuleDependencyRules() {
        // Utility class
    }

    /**
     * Cria uma regra que proíbe dependências em módulos específicos.
     *
     * @param sourceModule      Módulo de origem
     * @param forbiddenModules  Módulos que não podem ser dependências
     * @param modulePackageBase Pacote base dos módulos
     * @return Regra ArchUnit
     */
    public static ArchRule moduleShouldNotDependOn(
            String sourceModule,
            Set<String> forbiddenModules,
            String modulePackageBase) {

        String sourcePackage = modulePackageBase + "." + sourceModule + "..";
        String[] forbiddenPackages = forbiddenModules.stream()
                .map(m -> modulePackageBase + "." + m + "..")
                .toArray(String[]::new);

        return noClasses()
                .that().resideInAPackage(sourcePackage)
                .should().dependOnClassesThat()
                .resideInAnyPackage(forbiddenPackages)
                .because("Module '" + sourceModule + "' should not depend on: " + forbiddenModules);
    }

    /**
     * Cria uma regra que valida a direção das dependências (acíclico).
     *
     * @param modulePackagePattern Padrão de pacote dos módulos
     * @return Regra ArchUnit
     */
    public static ArchRule modulesShouldHaveAcyclicDependencies(String modulePackagePattern) {
        return SlicesRuleDefinition.slices()
                .matching(modulePackagePattern)
                .should().beFreeOfCycles();
    }

    /**
     * Cria uma regra que um módulo só pode depender de módulos específicos.
     *
     * @param sourceModule      Módulo de origem
     * @param allowedModules    Módulos permitidos como dependências
     * @param modulePackageBase Pacote base dos módulos
     * @return Regra ArchUnit
     */
    public static ArchRule moduleShouldOnlyDependOn(
            String sourceModule,
            Set<String> allowedModules,
            String modulePackageBase) {

        String sourcePackage = modulePackageBase + "." + sourceModule + "..";
        String[] allowedPackages = allowedModules.stream()
                .map(m -> modulePackageBase + "." + m + "..")
                .toArray(String[]::new);

        // Add common packages that are always allowed
        List<String> allAllowed = new ArrayList<>(Arrays.asList(allowedPackages));
        allAllowed.add("java..");
        allAllowed.add("javax..");
        allAllowed.add("jakarta..");
        allAllowed.add("org.springframework..");
        allAllowed.add(sourcePackage); // Self-dependency is allowed

        return noClasses()
                .that().resideInAPackage(sourcePackage)
                .should().dependOnClassesThat()
                .resideOutsideOfPackages(allAllowed.toArray(new String[0]))
                .because("Module '" + sourceModule + "' should only depend on: " + allowedModules);
    }

    /**
     * Builder para construir regras de dependência de forma fluente.
     *
     * @param modulePackageBase Pacote base dos módulos
     * @return Builder
     */
    public static DependencyRuleBuilder forModules(String modulePackageBase) {
        return new DependencyRuleBuilder(modulePackageBase);
    }

    /**
     * Builder para construção fluente de regras de dependência.
     */
    public static class DependencyRuleBuilder {
        private final String modulePackageBase;
        private final Map<String, Set<String>> forbiddenDependencies = new HashMap<>();
        private final Map<String, Set<String>> allowedDependencies = new HashMap<>();

        DependencyRuleBuilder(String modulePackageBase) {
            this.modulePackageBase = modulePackageBase;
        }

        /**
         * Define que um módulo NÃO pode depender de outro.
         *
         * @param sourceModule Módulo de origem
         * @param targetModule Módulo que não pode ser dependência
         * @return this
         */
        public DependencyRuleBuilder forbid(String sourceModule, String targetModule) {
            forbiddenDependencies
                    .computeIfAbsent(sourceModule, k -> new HashSet<>())
                    .add(targetModule);
            return this;
        }

        /**
         * Define que um módulo pode depender de outro.
         *
         * @param sourceModule Módulo de origem
         * @param targetModule Módulo de destino permitido
         * @return this
         */
        public DependencyRuleBuilder allow(String sourceModule, String targetModule) {
            allowedDependencies
                    .computeIfAbsent(sourceModule, k -> new HashSet<>())
                    .add(targetModule);
            return this;
        }

        /**
         * Define que um módulo pode depender de vários outros.
         *
         * @param sourceModule  Módulo de origem
         * @param targetModules Módulos de destino permitidos
         * @return this
         */
        public DependencyRuleBuilder allow(String sourceModule, String... targetModules) {
            allowedDependencies
                    .computeIfAbsent(sourceModule, k -> new HashSet<>())
                    .addAll(Arrays.asList(targetModules));
            return this;
        }

        /**
         * Constrói e verifica todas as regras contra as classes fornecidas.
         *
         * @param classes Classes a serem verificadas
         */
        public void check(JavaClasses classes) {
            // Check forbidden dependencies
            for (Map.Entry<String, Set<String>> entry : forbiddenDependencies.entrySet()) {
                moduleShouldNotDependOn(entry.getKey(), entry.getValue(), modulePackageBase)
                        .check(classes);
            }

            // Check allowed dependencies (if specified)
            for (Map.Entry<String, Set<String>> entry : allowedDependencies.entrySet()) {
                moduleShouldOnlyDependOn(entry.getKey(), entry.getValue(), modulePackageBase)
                        .check(classes);
            }
        }
    }
}
