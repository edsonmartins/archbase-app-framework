package br.com.archbase.architecture.rules.spring;

import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Regras de arquitetura para padrões Spring do Archbase.
 * <p>
 * Valida que o código segue as boas práticas Spring:
 * - Nomenclatura correta de componentes
 * - Injeção de dependência via construtor
 * - Separação de responsabilidades entre camadas
 * - Anotações de segurança em controllers
 *
 * @author Archbase Team
 * @since 2.0.1
 */
public final class ArchbaseSpringRules {

    private ArchbaseSpringRules() {
        // Utility class
    }

    /**
     * Configura regras Spring no builder Taikai.
     *
     * @param builder Taikai builder
     */
    public static void configure(Taikai.Builder builder) {
        builder.spring(spring -> {
            // Não usar @Autowired em campos
            spring.noAutowiredFields();

            // Regras para Controllers
            spring.controllers(controllers -> {
                controllers.shouldBeAnnotatedWithRestController();
                controllers.namesShouldEndWithController();
                controllers.shouldNotDependOnOtherControllers();
            });

            // Regras para Services
            spring.services(services -> {
                services.namesShouldEndWithService();
                services.shouldNotDependOnControllers();
            });

            // Regras para Repositories
            spring.repositories(repositories -> {
                repositories.namesShouldEndWithRepository();
                repositories.shouldNotDependOnServices();
            });

            // Regras para Configurations
            spring.configurations(configs -> {
                configs.namesShouldEndWithConfiguration();
            });
        });

        // Regras customizadas adicionais
        builder.addRule(TaikaiRule.of(servicesShouldNotAccessRepositoriesDirectlyFromControllers()));
        builder.addRule(TaikaiRule.of(controllersShouldNotContainBusinessLogic()));
    }

    /**
     * Configura regras de segurança Archbase.
     *
     * @param builder   Taikai builder
     * @param namespace namespace do projeto
     */
    public static void configureSecurityRules(Taikai.Builder builder, String namespace) {
        // Regra: Controllers REST devem ter anotações de segurança
        builder.addRule(TaikaiRule.of(controllerMethodsShouldHaveSecurityAnnotations()));

        // Regra: Endpoints devem ter segurança explícita
        builder.addRule(TaikaiRule.of(restEndpointsShouldHaveExplicitSecurity()));
    }

    /**
     * Regra: Controllers não devem acessar Repositories diretamente.
     */
    public static ArchRule servicesShouldNotAccessRepositoriesDirectlyFromControllers() {
        return ArchRuleDefinition.noClasses()
                .that().areAnnotatedWith(RestController.class)
                .or().areAnnotatedWith(Controller.class)
                .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
                .because("Controllers devem acessar dados através de Services, " +
                        "não diretamente de Repositories (separação de responsabilidades)");
    }

    /**
     * Regra: Controllers não devem conter lógica de negócio.
     */
    public static ArchRule controllersShouldNotContainBusinessLogic() {
        return ArchRuleDefinition.classes()
                .that().areAnnotatedWith(RestController.class)
                .or().areAnnotatedWith(Controller.class)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "jakarta..",
                        "org.springframework..",
                        "org.slf4j..",
                        "io.swagger..",
                        "..dto..",
                        "..service..",
                        "..exception..",
                        "br.com.archbase.."
                )
                .because("Controllers devem apenas orquestrar chamadas, " +
                        "delegando lógica de negócio para Services");
    }

    /**
     * Regra: Métodos de controllers devem ter anotações de segurança.
     */
    public static ArchRule controllerMethodsShouldHaveSecurityAnnotations() {
        return ArchRuleDefinition.methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .and().areNotStatic()
                .should().beAnnotatedWith("br.com.archbase.security.annotation.HasPermission")
                .orShould().beAnnotatedWith("jakarta.annotation.security.PermitAll")
                .orShould().beAnnotatedWith("org.springframework.security.access.prepost.PreAuthorize")
                .because("Todos os endpoints REST devem ter segurança explícita " +
                        "via @HasPermission, @PermitAll ou @PreAuthorize");
    }

    /**
     * Regra: Endpoints REST devem ter segurança explícita.
     */
    public static ArchRule restEndpointsShouldHaveExplicitSecurity() {
        return ArchRuleDefinition.classes()
                .that().areAnnotatedWith(RestController.class)
                .should().beAnnotatedWith("org.springframework.security.access.annotation.Secured")
                .orShould().beAnnotatedWith("br.com.archbase.security.annotation.HasPermission")
                .orShould().beAnnotatedWith("jakarta.annotation.security.RolesAllowed")
                .orShould().haveSimpleNameContaining("Public")
                .because("Controllers REST devem ter segurança definida em nível de classe " +
                        "ou ser explicitamente marcados como públicos");
    }

    /**
     * Regra: Services não devem depender de Controllers.
     */
    public static ArchRule servicesShouldNotDependOnControllers() {
        return ArchRuleDefinition.noClasses()
                .that().areAnnotatedWith(Service.class)
                .should().dependOnClassesThat().areAnnotatedWith(RestController.class)
                .orShould().dependOnClassesThat().areAnnotatedWith(Controller.class)
                .because("Services não devem ter dependência de Controllers (inversão de dependências)");
    }

    /**
     * Regra: Repositories não devem depender de Services.
     */
    public static ArchRule repositoriesShouldNotDependOnServices() {
        return ArchRuleDefinition.noClasses()
                .that().areAnnotatedWith(Repository.class)
                .should().dependOnClassesThat().areAnnotatedWith(Service.class)
                .because("Repositories não devem ter dependência de Services (camadas inferiores " +
                        "não devem depender de camadas superiores)");
    }

    /**
     * Regra: Não usar @Autowired em campos.
     */
    public static ArchRule noAutowiredOnFields() {
        return ArchRuleDefinition.noFields()
                .should().beAnnotatedWith(Autowired.class)
                .because("Prefira injeção de dependência via construtor ao invés de @Autowired em campos " +
                        "(facilita testes e torna dependências explícitas)");
    }

    /**
     * Regra: Controllers devem ser package-private quando possível.
     */
    public static ArchRule controllersShouldBePackagePrivate() {
        return ArchRuleDefinition.classes()
                .that().areAnnotatedWith(RestController.class)
                .should().bePackagePrivate()
                .because("Controllers devem ter visibilidade mínima necessária (package-private) " +
                        "para melhor encapsulamento");
    }
}
