package br.com.archbase.modulith.rules;

import br.com.archbase.modulith.annotations.InternalApi;
import br.com.archbase.modulith.annotations.Module;
import br.com.archbase.modulith.annotations.ModuleApi;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras ArchUnit para enforcement de boundaries entre módulos.
 * <p>
 * Estas regras garantem que os módulos respeitem seus contratos e
 * não violem o encapsulamento de outros módulos.
 * <p>
 * Exemplo de uso em testes:
 * <pre>
 * {@code
 * @AnalyzeClasses(packages = "com.myapp")
 * public class ModularArchitectureTest {
 *
 *     @ArchTest
 *     static final ArchRule modules_should_not_have_cycles =
 *         ModuleBoundaryRules.modulesShouldNotHaveCyclicDependencies("com.myapp.modules.(*)");
 *
 *     @ArchTest
 *     static final ArchRule internal_apis_should_be_encapsulated =
 *         ModuleBoundaryRules.internalApisShouldNotBeAccessedFromOutside();
 * }
 * }
 * </pre>
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public final class ModuleBoundaryRules {

    private ModuleBoundaryRules() {
        // Utility class
    }

    /**
     * Regra: Módulos não devem ter dependências cíclicas.
     *
     * @param slicePattern Padrão para identificar módulos (ex: "com.myapp.modules.(*)..")
     * @return Regra ArchUnit
     */
    public static ArchRule modulesShouldNotHaveCyclicDependencies(String slicePattern) {
        return SlicesRuleDefinition.slices()
                .matching(slicePattern)
                .should().beFreeOfCycles();
    }

    /**
     * Regra: APIs internas (@InternalApi) devem estar em pacotes internos.
     *
     * @return Regra ArchUnit
     */
    public static ArchRule internalApisShouldResideInInternalPackages() {
        return classes()
                .that().areAnnotatedWith(InternalApi.class)
                .should().resideInAPackage("..internal..")
                .because("Internal APIs should be in internal packages");
    }

    /**
     * Regra: Classes em pacotes internos não devem ser públicas.
     *
     * @return Regra ArchUnit
     */
    public static ArchRule internalPackageClassesShouldNotBePublic() {
        return noClasses()
                .that().resideInAPackage("..internal..")
                .and().areNotAnnotatedWith(ModuleApi.class)
                .should().bePublic()
                .because("Classes in internal packages should not be public unless marked as @ModuleApi");
    }

    /**
     * Regra: Classes anotadas com @Module devem ser classes de configuração.
     *
     * @return Regra ArchUnit
     */
    public static ArchRule moduleClassesShouldBeConfigurations() {
        return classes()
                .that().areAnnotatedWith(Module.class)
                .should().beAnnotatedWith("org.springframework.context.annotation.Configuration")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                .because("Module classes should be Spring configurations or components");
    }

    /**
     * Regra: APIs de módulo (@ModuleApi) devem ser interfaces.
     *
     * @return Regra ArchUnit
     */
    public static ArchRule moduleApisShouldBeInterfaces() {
        return classes()
                .that().areAnnotatedWith(ModuleApi.class)
                .should().beInterfaces()
                .because("Module APIs should be interfaces for loose coupling");
    }

    /**
     * Regra: Eventos de integração devem ser imutáveis (records ou classes com campos finais).
     *
     * @return Regra ArchUnit
     */
    public static ArchRule integrationEventsShouldBeImmutable() {
        return classes()
                .that().areAnnotatedWith(br.com.archbase.modulith.annotations.IntegrationEvent.class)
                .should().beRecords()
                .orShould().haveOnlyFinalFields()
                .because("Integration events must be immutable for safe cross-module communication");
    }

    /**
     * Regra: Módulos não devem depender de pacotes internos de outros módulos.
     *
     * @param basePackage Pacote base dos módulos
     * @return Regra ArchUnit
     */
    public static ArchRule modulesShouldNotAccessOtherModulesInternals(String basePackage) {
        return noClasses()
                .that().resideInAPackage(basePackage + "..")
                .should().dependOnClassesThat()
                .resideInAPackage("..internal..")
                .because("Modules should not depend on internal packages of other modules");
    }

    /**
     * Verifica todas as regras de boundary contra um conjunto de classes.
     *
     * @param classes       Classes a serem verificadas
     * @param modulePattern Padrão para identificar módulos
     */
    public static void checkAll(JavaClasses classes, String modulePattern) {
        modulesShouldNotHaveCyclicDependencies(modulePattern).check(classes);
        internalApisShouldResideInInternalPackages().check(classes);
        moduleApisShouldBeInterfaces().check(classes);
        integrationEventsShouldBeImmutable().check(classes);
    }
}
