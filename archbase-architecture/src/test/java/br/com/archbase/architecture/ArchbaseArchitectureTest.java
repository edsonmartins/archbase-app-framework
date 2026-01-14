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

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Testes de exemplo para validação de arquitetura com ArchUnit.
 * <p>
 * Copie este arquivo para seu projeto e ajuste os pacotes conforme necessário.
 * </p>
 *
 * <p><b>Exemplo de uso:</b></p>
 * <pre>{@code
 * @AnnotateWith(ArchbaseApplication.class)
 * public class MyApplication {
 *     // Classes marcadas com arquitetura hexagonal
 * }
 * }</pre>
 */
class ArchbaseArchitectureTest {

    /**
     * Teste exemplo para validar controllers não dependem de repositorios.
     */
    @Test
    void controllersShouldNotDependOnRepositories() {
        var rule = noClasses()
                .that().resideInAPackage("..interface..")
                .and().haveSimpleNameContaining("Controller")
                .should().dependOnClassesThat()
                .haveSimpleNameContaining("Repository")
                .because("Controllers devem depender apenas de services/application");

        JavaClasses classes = new ClassFileImporter().importPackages("com.suaempresa");
        rule.check(classes);
    }

    /**
     * Teste exemplo para validar que domínio não depende de infraestrutura.
     */
    @Test
    void domainShouldNotDependOnInfrastructure() {
        var rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("O domínio não deve depender da infraestrutura");

        JavaClasses classes = new ClassFileImporter().importPackages("com.suaempresa");
        rule.check(classes);
    }

    /**
     * Teste exemplo para validar classes de serviço.
     */
    @Test
    void servicesShouldBeNamedCorrectly() {
        var rule = classes()
                .that().resideInAPackage("..application..")
                .and().haveSimpleNameNotContaining("Controller")
                .should().haveSimpleNameContaining("Service")
                .orShould().haveSimpleNameContaining("UseCase")
                .because("Classes de aplicação devem ser Services ou UseCases");

        JavaClasses classes = new ClassFileImporter().importPackages("com.suaempresa");
        rule.check(classes);
    }
}
