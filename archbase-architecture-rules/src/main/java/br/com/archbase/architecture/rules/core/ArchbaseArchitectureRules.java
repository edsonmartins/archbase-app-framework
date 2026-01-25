package br.com.archbase.architecture.rules.core;

import br.com.archbase.architecture.rules.ddd.ArchbaseDddRules;
import br.com.archbase.architecture.rules.naming.ArchbaseNamingRules;
import br.com.archbase.architecture.rules.spring.ArchbaseSpringRules;
import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe principal para configurar e executar regras de arquitetura Archbase.
 * <p>
 * Esta classe utiliza Taikai (extensão do ArchUnit) para validar que o código
 * segue os padrões arquiteturais definidos pelo framework Archbase.
 * <p>
 * Exemplo de uso:
 * <pre>{@code
 * ArchbaseArchitectureRules.forNamespace("com.minhaempresa.meuprojeto")
 *     .withDddRules()
 *     .withSpringRules()
 *     .withNamingRules()
 *     .withSecurityRules()
 *     .check();
 * }</pre>
 *
 * @author Archbase Team
 * @since 2.0.1
 */
public class ArchbaseArchitectureRules {

    private final String namespace;
    private final List<TaikaiRule> customRules = new ArrayList<>();
    private boolean enableDddRules = false;
    private boolean enableSpringRules = false;
    private boolean enableNamingRules = false;
    private boolean enableSecurityRules = false;
    private boolean enableMultitenancyRules = false;
    private boolean enableTestRules = false;
    private boolean failOnEmpty = true;
    private boolean checkAll = false;

    private ArchbaseArchitectureRules(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Cria uma nova instância para o namespace especificado.
     *
     * @param namespace o pacote base a ser analisado (ex: "com.minhaempresa.meuprojeto")
     * @return builder para configurar as regras
     */
    public static ArchbaseArchitectureRules forNamespace(String namespace) {
        return new ArchbaseArchitectureRules(namespace);
    }

    /**
     * Habilita regras de Domain-Driven Design.
     * <p>
     * Valida:
     * - Entidades JPA devem estender TenantPersistenceEntityBase ou PersistenceEntityBase
     * - Repositórios devem implementar ArchbaseCommonJpaRepository
     * - Classes de domínio devem estar em pacotes corretos
     */
    public ArchbaseArchitectureRules withDddRules() {
        this.enableDddRules = true;
        return this;
    }

    /**
     * Habilita regras Spring.
     * <p>
     * Valida:
     * - Controllers devem terminar com "Controller"
     * - Services devem terminar com "Service"
     * - Repositories devem terminar com "Repository"
     * - Não usar @Autowired em campos (preferir constructor injection)
     * - Controllers não devem depender de outros controllers
     */
    public ArchbaseArchitectureRules withSpringRules() {
        this.enableSpringRules = true;
        return this;
    }

    /**
     * Habilita regras de nomenclatura.
     * <p>
     * Valida:
     * - Convenções de nomes para classes, métodos e constantes
     * - Interfaces não devem ter prefixo "I"
     * - Classes não devem ter sufixo "Impl" (preferir nomes descritivos)
     */
    public ArchbaseArchitectureRules withNamingRules() {
        this.enableNamingRules = true;
        return this;
    }

    /**
     * Habilita regras de segurança Archbase.
     * <p>
     * Valida:
     * - Controllers REST devem ter @HasPermission ou @PermitAll
     * - Endpoints públicos devem ser explicitamente marcados
     */
    public ArchbaseArchitectureRules withSecurityRules() {
        this.enableSecurityRules = true;
        return this;
    }

    /**
     * Habilita regras de multitenancy.
     * <p>
     * Valida:
     * - Entidades devem estender TenantPersistenceEntityBase
     * - Queries devem considerar tenant context
     */
    public ArchbaseArchitectureRules withMultitenancyRules() {
        this.enableMultitenancyRules = true;
        return this;
    }

    /**
     * Habilita regras de testes.
     * <p>
     * Valida:
     * - Testes devem conter assertions
     * - Testes não devem estar desabilitados (@Disabled)
     */
    public ArchbaseArchitectureRules withTestRules() {
        this.enableTestRules = true;
        return this;
    }

    /**
     * Habilita todas as regras disponíveis.
     */
    public ArchbaseArchitectureRules withAllRules() {
        return this
                .withDddRules()
                .withSpringRules()
                .withNamingRules()
                .withSecurityRules()
                .withMultitenancyRules()
                .withTestRules();
    }

    /**
     * Adiciona uma regra customizada.
     *
     * @param rule regra ArchUnit/Taikai customizada
     */
    public ArchbaseArchitectureRules addRule(TaikaiRule rule) {
        this.customRules.add(rule);
        return this;
    }

    /**
     * Define se deve falhar quando não encontrar classes para validar.
     * Padrão: true
     */
    public ArchbaseArchitectureRules failOnEmpty(boolean failOnEmpty) {
        this.failOnEmpty = failOnEmpty;
        return this;
    }

    /**
     * Define se deve coletar todos os erros antes de falhar (checkAll)
     * ou falhar no primeiro erro (check).
     * Padrão: false (falha no primeiro erro)
     */
    public ArchbaseArchitectureRules checkAll(boolean checkAll) {
        this.checkAll = checkAll;
        return this;
    }

    /**
     * Executa a validação das regras configuradas.
     *
     * @throws AssertionError se alguma regra for violada
     */
    public void check() {
        Taikai.Builder builder = Taikai.builder()
                .namespace(namespace)
                .failOnEmpty(failOnEmpty);

        // Configura regras Java base
        builder.java(java -> {
            java.noUsageOfDeprecatedAPIs();
            java.classesShouldImplementHashCodeAndEquals();
            java.imports(imports -> imports.shouldHaveNoCycles());
        });

        // Configura regras DDD
        if (enableDddRules) {
            ArchbaseDddRules.configure(builder, namespace);
        }

        // Configura regras Spring
        if (enableSpringRules) {
            ArchbaseSpringRules.configure(builder);
        }

        // Configura regras de nomenclatura
        if (enableNamingRules) {
            ArchbaseNamingRules.configure(builder);
        }

        // Configura regras de segurança
        if (enableSecurityRules) {
            ArchbaseSpringRules.configureSecurityRules(builder, namespace);
        }

        // Configura regras de multitenancy
        if (enableMultitenancyRules) {
            ArchbaseDddRules.configureMultitenancyRules(builder, namespace);
        }

        // Configura regras de testes
        if (enableTestRules) {
            builder.test(test -> test.junit5(junit -> {
                junit.classesShouldNotBeAnnotatedWithDisabled();
                junit.methodsShouldContainAssertionsOrVerifications();
            }));
        }

        // Adiciona regras customizadas
        for (TaikaiRule rule : customRules) {
            builder.addRule(rule);
        }

        // Executa validação
        Taikai taikai = builder.build();
        if (checkAll) {
            taikai.checkAll();
        } else {
            taikai.check();
        }
    }

    /**
     * Importa classes para uso em regras customizadas.
     *
     * @return JavaClasses importadas do namespace
     */
    public JavaClasses importClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(namespace);
    }
}
