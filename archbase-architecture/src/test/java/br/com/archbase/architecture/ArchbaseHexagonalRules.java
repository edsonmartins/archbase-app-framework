/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.archbase.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras ArchUnit para validar arquitetura Hexagonal.
 * <p>
 * Estas regras garantem que a arquitetura hexagonal é respeitada,
 * verificando que as camadas não violam suas dependências.
 * </p>
 *
 * <p><b>Uso:</b></p>
 * <pre>{@code
 * @AnnotateWith(ArchbaseApplication.class)
 * public class MyArchitectureTest {
 *     @Test
 *     void shouldValidateHexagonalRules() {
 *         ArchbaseHexagonalRules.hexagonalRules("com.minhaempresa")
 *             .check(new ClassFileImporter().importPackages("com.minhaempresa"));
 *     }
 * }
 * }</pre>
 */
public class ArchbaseHexagonalRules {

    /**
     * Retorna regras para validar arquitetura hexagonal.
     *
     * @param basePackage Pacote base da aplicação
     * @return regra ArchUnit configurada
     */
    public static ArchRule hexagonalRules(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..domain..", "..infrastructure..")
                .because("Apenas classes de application/domain/infrastructure devem existir dentro do pacote base");
    }

    /**
     * Retorna regras para validar que domínio não depende de infraestrutura.
     *
     * @param domainPackage Pacote do domínio
     * @param infrastructurePackage Pacote de infraestrutura
     * @return regra ArchUnit configurada
     */
    public static ArchRule domainShouldNotDependOnInfrastructure(String domainPackage, String infrastructurePackage) {
        return noClasses()
                .that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat()
                .resideInAPackage(infrastructurePackage)
                .because("O domínio não deve depender da infraestrutura");
    }

    /**
     * Retorna regras para validar que adapters não dependem uns dos outros indevidamente.
     *
     * @param primaryAdapterPackage Pacote de adapters primários
     * @param secondaryAdapterPackage Pacote de adapters secundários
     * @return regra ArchUnit configurada
     */
    public static ArchRule adaptersShouldFollowPortRules(String primaryAdapterPackage, String secondaryAdapterPackage) {
        return classes()
                .that().resideInAPackage(primaryAdapterPackage)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "jakarta..", "org.springframework..", "br.com.archbase..")
                .orShould().resideInAPackage(secondaryAdapterPackage)
                .because("Adapters primários devem depender apenas de ports ou outros adapters permitidos");
    }
}
