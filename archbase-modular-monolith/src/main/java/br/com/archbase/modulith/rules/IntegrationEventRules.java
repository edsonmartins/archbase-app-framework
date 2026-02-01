package br.com.archbase.modulith.rules;

import br.com.archbase.modulith.communication.contracts.IntegrationEvent;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Regras ArchUnit específicas para Integration Events.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
public final class IntegrationEventRules {

    private IntegrationEventRules() {
        // Utility class
    }

    /**
     * Regra: Integration Events devem implementar a interface IntegrationEvent.
     *
     * @return Regra ArchUnit
     */
    public static ArchRule integrationEventsShouldImplementInterface() {
        return classes()
                .that().areAnnotatedWith(br.com.archbase.modulith.annotations.IntegrationEvent.class)
                .should().implement(IntegrationEvent.class)
                .because("Integration events must implement IntegrationEvent interface");
    }

    /**
     * Regra: Integration Events devem ser serializáveis.
     *
     * @return Regra ArchUnit
     */
    public static ArchRule integrationEventsShouldBeSerializable() {
        return classes()
                .that().implement(IntegrationEvent.class)
                .should().implement(java.io.Serializable.class)
                .because("Integration events must be serializable for persistence in outbox");
    }

    /**
     * Regra: Integration Events devem ser records ou ter campos finais.
     *
     * @return Regra ArchUnit
     */
    public static ArchRule integrationEventsShouldBeImmutable() {
        return classes()
                .that().implement(IntegrationEvent.class)
                .should().beRecords()
                .orShould().haveOnlyFinalFields()
                .because("Integration events must be immutable");
    }

    /**
     * Regra: Integration Events devem ter nomes que terminam com "Event".
     *
     * @return Regra ArchUnit
     */
    public static ArchRule integrationEventsShouldHaveProperNaming() {
        return classes()
                .that().implement(IntegrationEvent.class)
                .should().haveSimpleNameEndingWith("Event")
                .because("Integration events should follow naming convention *Event");
    }

    /**
     * Regra: Integration Events devem residir em pacotes específicos.
     *
     * @param allowedPackages Pacotes permitidos para Integration Events
     * @return Regra ArchUnit
     */
    public static ArchRule integrationEventsShouldResideInSpecificPackages(String... allowedPackages) {
        return classes()
                .that().implement(IntegrationEvent.class)
                .should().resideInAnyPackage(allowedPackages)
                .because("Integration events should be organized in specific packages");
    }
}
