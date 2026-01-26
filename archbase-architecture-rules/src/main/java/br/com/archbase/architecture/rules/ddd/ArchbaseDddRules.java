package br.com.archbase.architecture.rules.ddd;

import br.com.archbase.ddd.domain.base.PersistenceEntityBase;
import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.ddd.domain.contracts.Repository;
import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import jakarta.persistence.Entity;

import java.util.Set;

/**
 * Regras de arquitetura para padrões DDD (Domain-Driven Design) do Archbase.
 * <p>
 * Valida que o código segue os padrões DDD definidos pelo framework:
 * - Entidades devem estender classes base apropriadas
 * - Repositórios devem implementar interfaces Archbase
 * - Repositórios não devem ter métodos customizados (usar QueryDSL)
 * - Separação correta entre camadas
 *
 * @author Archbase Team
 * @since 2.0.1
 */
public final class ArchbaseDddRules {

    /**
     * Métodos herdados que são permitidos em repositórios.
     * Qualquer método que não esteja nesta lista ou não seja herdado será considerado violação.
     */
    private static final Set<String> ALLOWED_REPOSITORY_METHOD_PREFIXES = Set.of(
            "findById", "findAll", "save", "saveAll", "delete", "deleteById",
            "deleteAll", "count", "existsById", "flush", "saveAndFlush",
            "findOne", "getById", "getReferenceById", "deleteAllById",
            "deleteAllInBatch", "deleteInBatch", "saveAllAndFlush"
    );

    private ArchbaseDddRules() {
        // Utility class
    }

    /**
     * Configura regras DDD no builder Taikai.
     *
     * @param builder   Taikai builder
     * @param namespace namespace do projeto
     */
    public static void configure(Taikai.Builder builder, String namespace) {
        // Regra: Entidades JPA devem estender PersistenceEntityBase ou TenantPersistenceEntityBase
        builder.addRule(TaikaiRule.of(entitiesShouldExtendPersistenceBase()));

        // Regra: Repositórios devem implementar Repository do Archbase
        builder.addRule(TaikaiRule.of(repositoriesShouldImplementArchbaseRepository()));

        // Regra: Repositórios NÃO devem ter métodos customizados (usar QueryDSL no adapter)
        builder.addRule(TaikaiRule.of(repositoriesShouldNotHaveCustomMethods()));

        // Regra: Camada de domínio não deve depender de infraestrutura
        builder.addRule(TaikaiRule.of(domainShouldNotDependOnInfrastructure(namespace)));

        // Regra: Entidades de domínio devem estar em pacotes corretos
        builder.addRule(TaikaiRule.of(entitiesShouldResideInCorrectPackage()));
    }

    /**
     * Configura regras de multitenancy.
     *
     * @param builder   Taikai builder
     * @param namespace namespace do projeto
     */
    public static void configureMultitenancyRules(Taikai.Builder builder, String namespace) {
        // Regra: Em projetos multitenancy, entidades devem estender TenantPersistenceEntityBase
        builder.addRule(TaikaiRule.of(entitiesShouldExtendTenantBase()));
    }

    /**
     * Regra: Entidades JPA devem estender PersistenceEntityBase ou TenantPersistenceEntityBase.
     */
    public static ArchRule entitiesShouldExtendPersistenceBase() {
        return ArchRuleDefinition.classes()
                .that().areAnnotatedWith(Entity.class)
                .should().beAssignableTo(PersistenceEntityBase.class)
                .because("Entidades JPA do Archbase devem estender PersistenceEntityBase " +
                        "ou TenantPersistenceEntityBase para herdar campos de auditoria e versionamento");
    }

    /**
     * Regra: Entidades devem estender TenantPersistenceEntityBase (para projetos multitenancy).
     */
    public static ArchRule entitiesShouldExtendTenantBase() {
        return ArchRuleDefinition.classes()
                .that().areAnnotatedWith(Entity.class)
                .should().beAssignableTo(TenantPersistenceEntityBase.class)
                .because("Em projetos multitenancy, entidades devem estender TenantPersistenceEntityBase " +
                        "para garantir isolamento de dados por tenant");
    }

    /**
     * Regra: Repositórios devem implementar Repository do Archbase.
     */
    public static ArchRule repositoriesShouldImplementArchbaseRepository() {
        return ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().beAssignableTo(Repository.class)
                .orShould().beAssignableTo(org.springframework.data.repository.Repository.class)
                .because("Repositórios devem implementar a interface Repository do Archbase " +
                        "para suporte a QueryDSL, RSQL e paginação");
    }

    /**
     * Regra: Repositórios NÃO devem ter métodos customizados.
     * <p>
     * Todas as consultas devem ser feitas usando QueryDSL no adapter,
     * não através de métodos de query derivados ou @Query no repositório.
     */
    public static ArchRule repositoriesShouldNotHaveCustomMethods() {
        return ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Repository")
                .or().haveSimpleNameEndingWith("JpaRepository")
                .and().areInterfaces()
                .should(notHaveCustomQueryMethods())
                .because("Repositórios não devem ter métodos customizados de query. " +
                        "Use QueryDSL no adapter para consultas complexas. " +
                        "Isso mantém os repositórios limpos e a lógica de consulta centralizada.");
    }

    /**
     * Condição que verifica se a interface de repositório não tem métodos customizados.
     */
    private static ArchCondition<JavaClass> notHaveCustomQueryMethods() {
        return new ArchCondition<>("não ter métodos customizados de query") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethod method : javaClass.getMethods()) {
                    // Ignora métodos default (implementados na interface)
                    if (method.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                        continue;
                    }

                    // Ignora métodos bridge
                    if (method.getModifiers().contains(JavaModifier.BRIDGE)) {
                        continue;
                    }

                    // Ignora métodos herdados de interfaces pai
                    if (isInheritedFromParentInterface(method, javaClass)) {
                        continue;
                    }

                    String methodName = method.getName();

                    // Verifica se é um método de query customizado (findBy*, queryBy*, etc)
                    if (isCustomQueryMethod(methodName)) {
                        String message = String.format(
                                "Repositório '%s' contém método customizado '%s'. " +
                                        "Use QueryDSL no adapter ao invés de métodos derivados no repositório.",
                                javaClass.getSimpleName(),
                                methodName
                        );
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    }

                    // Verifica se tem anotação @Query
                    if (method.isAnnotatedWith("org.springframework.data.jpa.repository.Query")) {
                        String message = String.format(
                                "Repositório '%s' contém método com @Query: '%s'. " +
                                        "Use QueryDSL no adapter ao invés de @Query no repositório.",
                                javaClass.getSimpleName(),
                                methodName
                        );
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    }
                }
            }
        };
    }

    /**
     * Verifica se o método é herdado de uma interface pai.
     */
    private static boolean isInheritedFromParentInterface(JavaMethod method, JavaClass javaClass) {
        for (JavaClass parent : javaClass.getAllRawInterfaces()) {
            for (JavaMethod parentMethod : parent.getMethods()) {
                if (parentMethod.getName().equals(method.getName()) &&
                        parentMethod.getRawParameterTypes().equals(method.getRawParameterTypes())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica se o nome do método indica um método de query customizado.
     */
    private static boolean isCustomQueryMethod(String methodName) {
        // Métodos que indicam queries customizadas
        return methodName.startsWith("findBy") ||
                methodName.startsWith("findAllBy") ||
                methodName.startsWith("queryBy") ||
                methodName.startsWith("readBy") ||
                methodName.startsWith("getBy") ||
                methodName.startsWith("countBy") ||
                methodName.startsWith("existsBy") ||
                methodName.startsWith("deleteBy") ||
                methodName.startsWith("removeBy") ||
                methodName.startsWith("searchBy") ||
                (methodName.contains("And") && methodName.startsWith("find")) ||
                (methodName.contains("Or") && methodName.startsWith("find")) ||
                methodName.contains("OrderBy") ||
                methodName.contains("Between") ||
                methodName.contains("LessThan") ||
                methodName.contains("GreaterThan") ||
                methodName.contains("Like") ||
                methodName.contains("Containing") ||
                methodName.contains("StartingWith") ||
                methodName.contains("EndingWith") ||
                methodName.contains("IgnoreCase") ||
                methodName.contains("IsNull") ||
                methodName.contains("IsNotNull") ||
                methodName.contains("In") && (methodName.startsWith("find") || methodName.startsWith("delete"));
    }

    /**
     * Regra: Camada de domínio não deve depender de infraestrutura.
     */
    public static ArchRule domainShouldNotDependOnInfrastructure(String namespace) {
        return ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("A camada de domínio deve ser independente de infraestrutura (Hexagonal Architecture)");
    }

    /**
     * Regra: Entidades devem residir em pacotes apropriados.
     */
    public static ArchRule entitiesShouldResideInCorrectPackage() {
        return ArchRuleDefinition.classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAnyPackage("..domain..", "..entity..", "..persistence..", "..model..")
                .because("Entidades devem estar organizadas em pacotes domain, entity, persistence ou model");
    }

    /**
     * Regra: Classes de domínio não devem ter dependências cíclicas.
     */
    public static ArchRule domainShouldNotHaveCyclicDependencies() {
        return ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..application..")
                .because("A camada de domínio não deve depender da camada de aplicação");
    }

    /**
     * Regra: Agregados não devem referenciar outros agregados diretamente.
     */
    public static ArchRule aggregatesShouldNotReferenceOtherAggregatesDirectly() {
        return ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Aggregate")
                .or().haveSimpleNameEndingWith("AggregateRoot")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "..domain..", "..value..", "br.com.archbase..")
                .because("Agregados devem referenciar outros agregados apenas por ID, não por referência direta");
    }
}
