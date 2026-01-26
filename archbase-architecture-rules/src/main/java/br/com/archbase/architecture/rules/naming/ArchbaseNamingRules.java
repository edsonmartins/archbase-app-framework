package br.com.archbase.architecture.rules.naming;

import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Regras de nomenclatura para projetos Archbase.
 * <p>
 * Define convenções de nomes para classes, interfaces, métodos e constantes
 * seguindo as boas práticas do framework Archbase.
 *
 * @author Archbase Team
 * @since 2.0.1
 */
public final class ArchbaseNamingRules {

    private ArchbaseNamingRules() {
        // Utility class
    }

    /**
     * Configura regras de nomenclatura no builder Taikai.
     *
     * @param builder Taikai builder
     */
    public static void configure(Taikai.Builder builder) {
        builder.java(java -> java.naming(naming -> {
            // Interfaces não devem ter prefixo "I"
            naming.interfacesShouldNotHavePrefixI();

            // Constantes devem seguir UPPER_SNAKE_CASE
            naming.constantsShouldFollowConventions();

            // Pacotes devem estar em lowercase
            naming.packagesShouldMatchDefault();
        }));

        // Regras customizadas
        builder.addRule(TaikaiRule.of(classesShouldNotEndWithImpl()));
        builder.addRule(TaikaiRule.of(dtoClassesShouldEndWithDto()));
        builder.addRule(TaikaiRule.of(exceptionsShouldEndWithException()));
        builder.addRule(TaikaiRule.of(testClassesShouldEndWithTest()));
    }

    /**
     * Regra: Classes não devem terminar com "Impl".
     * <p>
     * Prefira nomes descritivos como "JpaUserRepository" ao invés de "UserRepositoryImpl".
     */
    public static ArchRule classesShouldNotEndWithImpl() {
        return ArchRuleDefinition.noClasses()
                .should().haveSimpleNameEndingWith("Impl")
                .because("Classes não devem usar sufixo 'Impl'. " +
                        "Use nomes descritivos como 'JpaUserRepository' ao invés de 'UserRepositoryImpl'");
    }

    /**
     * Regra: DTOs devem terminar com "DTO" ou "Dto".
     */
    public static ArchRule dtoClassesShouldEndWithDto() {
        return ArchRuleDefinition.classes()
                .that().resideInAPackage("..dto..")
                .should().haveSimpleNameEndingWith("DTO")
                .orShould().haveSimpleNameEndingWith("Dto")
                .because("Classes de transferência de dados devem terminar com 'DTO' ou 'Dto' " +
                        "para identificação clara de sua função");
    }

    /**
     * Regra: Exceções devem terminar com "Exception".
     */
    public static ArchRule exceptionsShouldEndWithException() {
        return ArchRuleDefinition.classes()
                .that().areAssignableTo(Exception.class)
                .should().haveSimpleNameEndingWith("Exception")
                .because("Classes de exceção devem terminar com 'Exception' para identificação clara");
    }

    /**
     * Regra: Classes de teste devem terminar com "Test" ou "Tests".
     */
    public static ArchRule testClassesShouldEndWithTest() {
        return ArchRuleDefinition.classes()
                .that().resideInAPackage("..test..")
                .or().resideInAPackage("..tests..")
                .should().haveSimpleNameEndingWith("Test")
                .orShould().haveSimpleNameEndingWith("Tests")
                .orShould().haveSimpleNameEndingWith("IT")
                .because("Classes de teste devem terminar com 'Test', 'Tests' ou 'IT' (integration test)");
    }

    /**
     * Regra: Interfaces não devem ter prefixo "I".
     */
    public static ArchRule interfacesShouldNotHavePrefixI() {
        return ArchRuleDefinition.noClasses()
                .that().areInterfaces()
                .should().haveSimpleNameStartingWith("I")
                .because("Interfaces não devem ter prefixo 'I' (convenção Java). " +
                        "Use 'UserRepository' ao invés de 'IUserRepository'");
    }

    /**
     * Regra: Enums devem estar em pacotes apropriados.
     */
    public static ArchRule enumsShouldResideInCorrectPackages() {
        return ArchRuleDefinition.classes()
                .that().areEnums()
                .should().resideInAnyPackage("..enums..", "..domain..", "..model..", "..type..")
                .because("Enumerações devem estar organizadas em pacotes 'enums', 'domain', 'model' ou 'type'");
    }

    /**
     * Regra: Mappers devem terminar com "Mapper".
     */
    public static ArchRule mappersShouldEndWithMapper() {
        return ArchRuleDefinition.classes()
                .that().resideInAPackage("..mapper..")
                .should().haveSimpleNameEndingWith("Mapper")
                .because("Classes de mapeamento devem terminar com 'Mapper' para identificação clara");
    }

    /**
     * Regra: Adapters devem terminar com "Adapter".
     */
    public static ArchRule adaptersShouldEndWithAdapter() {
        return ArchRuleDefinition.classes()
                .that().resideInAPackage("..adapter..")
                .should().haveSimpleNameEndingWith("Adapter")
                .because("Classes adapter devem terminar com 'Adapter' " +
                        "conforme padrão Hexagonal Architecture");
    }

    /**
     * Regra: Ports devem terminar com "Port".
     */
    public static ArchRule portsShouldEndWithPort() {
        return ArchRuleDefinition.classes()
                .that().resideInAPackage("..port..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Port")
                .because("Interfaces de port devem terminar com 'Port' " +
                        "conforme padrão Hexagonal Architecture");
    }

    /**
     * Regra: Value Objects devem estar em pacotes apropriados.
     */
    public static ArchRule valueObjectsShouldResideInCorrectPackages() {
        return ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("VO")
                .or().haveSimpleNameEndingWith("ValueObject")
                .should().resideInAnyPackage("..domain..", "..value..", "..vo..")
                .because("Value Objects devem estar em pacotes 'domain', 'value' ou 'vo'");
    }
}
