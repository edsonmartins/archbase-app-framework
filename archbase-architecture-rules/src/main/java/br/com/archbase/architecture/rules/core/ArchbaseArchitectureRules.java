package br.com.archbase.architecture.rules.core;

import br.com.archbase.architecture.rules.ddd.ArchbaseDddRules;
import br.com.archbase.architecture.rules.naming.ArchbaseNamingRules;
import br.com.archbase.architecture.rules.spring.ArchbaseSpringRules;
import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import br.com.archbase.architecture.rules.architecture.ArchbaseArchitectureStyleRules;
import br.com.archbase.architecture.rules.ddd.ArchbaseAggregateRules;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    private boolean enableAggregateRules = false;
    private final Set<ArchitectureStyle> architectureStyles = EnumSet.noneOf(ArchitectureStyle.class);
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
     * Habilita as regras de modelagem de agregados, verificadas por tipo e anotação
     * ({@link ArchbaseAggregateRules}):
     * <ul>
     * <li>entidades só são referenciadas dentro do agregado a que pertencem;</li>
     * <li>tipos de domínio referenciam outros agregados por identificador ou associação.</li>
     * </ul>
     * Não faz parte de {@link #withAllRules()}: em modelos em que a entidade JPA é o próprio agregado, cada
     * {@code @ManyToOne} para outro agregado é uma violação, então ligar exige decisão consciente.
     *
     * @since 3.4.6
     */
    public ArchbaseArchitectureRules withAggregateRules() {
        this.enableAggregateRules = true;
        return this;
    }

    /**
     * Faz valer as anotações de estilo arquitetural do módulo {@code archbase-architecture}
     * ({@code @DomainLayer}, {@code @DomainRing}, {@code @PrimaryPort} etc.). Tipos sem essas anotações (no próprio
     * tipo ou em algum {@code package-info} acima dele) não são afetados.
     * <p>
     * Não faz parte de {@link #withAllRules()}.
     *
     * @param styles estilos a verificar
     * @see ArchbaseArchitectureStyleRules
     * @since 3.4.6
     */
    public ArchbaseArchitectureRules withArchitectureStyleRules(ArchitectureStyle... styles) {
        this.architectureStyles.addAll(List.of(styles));
        return this;
    }

    /**
     * Estilos arquiteturais verificáveis por {@link #withArchitectureStyleRules(ArchitectureStyle...)}.
     */
    public enum ArchitectureStyle {
        /** Camadas (Evans), cada camada depende de si ou de qualquer camada inferior. */
        LAYERED,
        /** Camadas (Evans), cada camada depende de si ou só da camada imediatamente inferior. */
        LAYERED_STRICT,
        /** Onion clássica (Palermo): domain model, domain services, application services, infrastructure. */
        ONION_CLASSICAL,
        /** Onion simplificada: domain, application, infrastructure. */
        ONION_SIMPLIFIED,
        /** Hexagonal (ports and adapters). */
        HEXAGONAL
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

        // Regras de agregado (opcionais, fora de withAllRules)
        if (enableAggregateRules) {
            builder.addRule(TaikaiRule.of(ArchbaseAggregateRules.entitiesShouldBeDeclaredForUseInSameAggregate()));
            builder.addRule(TaikaiRule.of(ArchbaseAggregateRules.aggregateReferencesShouldBeViaIdOrAssociation()));
        }

        // Regras de estilo arquitetural (opcionais, fora de withAllRules)
        for (ArchitectureStyle style : architectureStyles) {
            builder.addRule(TaikaiRule.of(switch (style) {
                case LAYERED -> ArchbaseArchitectureStyleRules.layered();
                case LAYERED_STRICT -> ArchbaseArchitectureStyleRules.layeredStrict();
                case ONION_CLASSICAL -> ArchbaseArchitectureStyleRules.onionClassical();
                case ONION_SIMPLIFIED -> ArchbaseArchitectureStyleRules.onionSimplified();
                case HEXAGONAL -> ArchbaseArchitectureStyleRules.hexagonal();
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
