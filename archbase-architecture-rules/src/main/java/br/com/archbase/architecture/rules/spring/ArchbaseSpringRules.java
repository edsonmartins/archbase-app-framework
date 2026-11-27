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

    /** Anotações que o Archbase aplica num endpoint, no método ou na classe. */
    private static final java.util.List<String> NO_METODO = java.util.List.of(
            "br.com.archbase.security.annotation.HasPermission",
            "org.springframework.security.access.prepost.PreAuthorize");
    private static final java.util.List<String> NO_METODO_OU_NA_CLASSE = java.util.List.of(
            "br.com.archbase.security.annotations.RequireProfile",
            "br.com.archbase.security.annotations.RequireRole",
            "br.com.archbase.security.annotations.RequirePersona");

    /**
     * Regra: Métodos de controllers devem ter anotações de segurança que o Archbase aplica — {@code @HasPermission} ou
     * {@code @PreAuthorize} no método, ou {@code @RequireProfile}/{@code @RequireRole}/{@code @RequirePersona} no
     * método ou na classe.
     *
     * <p>Até a 3.5.0 aceitava {@code @PermitAll}, que o Archbase ignora (só a segurança {@code prePost} está ligada):
     * a regra passava e o endpoint ficava protegido só pela cadeia de filtros. E não aceitava as anotações de classe.
     */
    public static ArchRule controllerMethodsShouldHaveSecurityAnnotations() {
        return ArchRuleDefinition.methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .and().areNotStatic()
                .should(terSegurancaQueOArchbaseAplica())
                .because("Todos os endpoints REST devem ter segurança explícita que o Archbase aplica "
                        + "(@HasPermission, @PreAuthorize ou @RequireProfile/@RequireRole/@RequirePersona); "
                        + "@PermitAll, @RolesAllowed e @Secured são ignorados");
    }

    /**
     * Regra: Endpoints REST devem ter segurança explícita — em todos os métodos (ver
     * {@link #controllerMethodsShouldHaveSecurityAnnotations()}), na classe, ou a classe marcada como pública pelo nome.
     *
     * <p>Até a 3.5.0 exigia na classe {@code @Secured}, {@code @RolesAllowed} (ignorados pelo Archbase) ou
     * {@code @HasPermission} (que só vale em método): na prática, só passava com "Public" no nome.
     */
    public static ArchRule restEndpointsShouldHaveExplicitSecurity() {
        return ArchRuleDefinition.classes()
                .that().areAnnotatedWith(RestController.class)
                .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>(
                        "ter segurança explícita na classe ou em todos os métodos públicos") {
                    @Override
                    public void check(com.tngtech.archunit.core.domain.JavaClass classe,
                                      com.tngtech.archunit.lang.ConditionEvents events) {
                        if (classe.getSimpleName().contains("Public") || temAlguma(classe, NO_METODO_OU_NA_CLASSE)) {
                            return;
                        }
                        for (var metodo : classe.getMethods()) {
                            if (metodo.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC)
                                    && !metodo.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.STATIC)
                                    && !temAlguma(metodo, NO_METODO) && !temAlguma(metodo, NO_METODO_OU_NA_CLASSE)) {
                                events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(classe,
                                        classe.getName() + " tem o endpoint " + metodo.getName()
                                                + " sem segurança que o Archbase aplica"));
                                return;
                            }
                        }
                    }
                })
                .because("Controllers REST devem ter segurança definida em nível de classe, em todos os métodos, "
                        + "ou ser explicitamente marcados como públicos");
    }

    private static com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaMethod> terSegurancaQueOArchbaseAplica() {
        return new com.tngtech.archunit.lang.ArchCondition<>("ter segurança que o Archbase aplica") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaMethod metodo,
                              com.tngtech.archunit.lang.ConditionEvents events) {
                boolean seguro = temAlguma(metodo, NO_METODO) || temAlguma(metodo, NO_METODO_OU_NA_CLASSE)
                        || temAlguma(metodo.getOwner(), NO_METODO_OU_NA_CLASSE);
                if (!seguro) {
                    events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(metodo,
                            metodo.getFullName() + " não tem segurança que o Archbase aplica"));
                }
            }
        };
    }

    private static boolean temAlguma(com.tngtech.archunit.core.domain.properties.CanBeAnnotated elemento,
                                     java.util.List<String> anotacoes) {
        return anotacoes.stream().anyMatch(elemento::isAnnotatedWith);
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
