/**
 * Archbase Architecture Rules - Validação de padrões arquiteturais.
 * <p>
 * Este módulo fornece regras predefinidas baseadas em Taikai/ArchUnit para
 * validar que projetos seguem os padrões arquiteturais do framework Archbase.
 *
 * <h2>Uso Básico</h2>
 * <pre>{@code
 * // Em um teste JUnit
 * @Test
 * void shouldFollowArchbasePatterns() {
 *     ArchbaseArchitectureRules.forNamespace("com.minhaempresa.meuprojeto")
 *         .withDddRules()
 *         .withSpringRules()
 *         .withSecurityRules()
 *         .check();
 * }
 * }</pre>
 *
 * <h2>Usando Profiles</h2>
 * <pre>{@code
 * // Para API REST com multitenancy
 * ArchbaseRuleProfiles.multitenantRestApi("com.minhaempresa.meuprojeto").check();
 *
 * // Para microservice simples
 * ArchbaseRuleProfiles.simpleService("com.minhaempresa.meuprojeto").check();
 * }</pre>
 *
 * <h2>Estendendo Classe Base</h2>
 * <pre>{@code
 * public class MinhaArquiteturaTest extends ArchbaseArchitectureTest {
 *
 *     @Override
 *     protected String getBasePackage() {
 *         return "com.minhaempresa.meuprojeto";
 *     }
 *
 *     @Override
 *     protected boolean enableSecurityRules() {
 *         return true;
 *     }
 * }
 * }</pre>
 *
 * <h2>Regras Disponíveis</h2>
 * <ul>
 *   <li><b>DDD Rules:</b> Entidades, repositórios, agregados, separação de camadas</li>
 *   <li><b>Spring Rules:</b> Controllers, services, repositories, injeção de dependência</li>
 *   <li><b>Naming Rules:</b> Convenções de nomenclatura para classes, interfaces, etc.</li>
 *   <li><b>Security Rules:</b> Validação de @HasPermission em endpoints</li>
 *   <li><b>Multitenancy Rules:</b> Entidades devem usar TenantPersistenceEntityBase</li>
 *   <li><b>Test Rules:</b> Testes devem ter assertions, não usar @Disabled</li>
 * </ul>
 *
 * @author Archbase Team
 * @since 2.0.1
 * @see br.com.archbase.architecture.rules.core.ArchbaseArchitectureRules
 * @see br.com.archbase.architecture.rules.core.ArchbaseRuleProfiles
 * @see br.com.archbase.architecture.rules.test.ArchbaseArchitectureTest
 */
package br.com.archbase.architecture.rules;
