/**
 * Archbase Modular Monolith - Suporte a arquitetura Modular Monolith.
 * <p>
 * Este módulo fornece suporte completo para construção de aplicações
 * usando a arquitetura Modular Monolith, incluindo:
 * <ul>
 *   <li><b>Anotações</b> - {@link br.com.archbase.modulith.annotations} para definir módulos</li>
 *   <li><b>Core</b> - {@link br.com.archbase.modulith.core} para registro e lifecycle</li>
 *   <li><b>Comunicação</b> - {@link br.com.archbase.modulith.communication} para integração entre módulos</li>
 *   <li><b>Regras</b> - {@link br.com.archbase.modulith.rules} para enforcement de arquitetura</li>
 *   <li><b>Spring</b> - {@link br.com.archbase.modulith.spring} para auto-configuração</li>
 * </ul>
 * <p>
 * Para habilitar o suporte a Modular Monolith em sua aplicação:
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableModularMonolith(basePackages = "com.myapp.modules")
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * }
 * </pre>
 * <p>
 * Baseado nos conceitos de Kamil Grzybek sobre Modular Monolith:
 * <ul>
 *   <li>Modular Monolith Primer</li>
 *   <li>Domain-Centric Design</li>
 *   <li>Integration Styles</li>
 *   <li>Architecture Enforcement</li>
 * </ul>
 *
 * @author Archbase Team
 * @since 3.0.0
 * @see br.com.archbase.modulith.spring.EnableModularMonolith
 * @see br.com.archbase.modulith.annotations.Module
 */
package br.com.archbase.modulith;
