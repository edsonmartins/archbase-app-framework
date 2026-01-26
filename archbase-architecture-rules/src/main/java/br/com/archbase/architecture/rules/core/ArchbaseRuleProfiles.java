package br.com.archbase.architecture.rules.core;

/**
 * Profiles predefinidos de regras para diferentes tipos de projetos Archbase.
 * <p>
 * Cada profile é otimizado para um tipo específico de projeto,
 * habilitando apenas as regras relevantes para aquele contexto.
 * <p>
 * Exemplo de uso:
 * <pre>{@code
 * // Para um projeto REST com multitenancy
 * ArchbaseRuleProfiles.multitenantRestApi("com.minhaempresa.meuprojeto").check();
 *
 * // Para um microservico simples
 * ArchbaseRuleProfiles.simpleService("com.minhaempresa.meuprojeto").check();
 * }</pre>
 *
 * @author Archbase Team
 * @since 2.0.1
 */
public final class ArchbaseRuleProfiles {

    private ArchbaseRuleProfiles() {
        // Utility class
    }

    /**
     * Profile para API REST completa com multitenancy e segurança.
     * <p>
     * Inclui:
     * - Regras DDD
     * - Regras Spring
     * - Regras de nomenclatura
     * - Regras de segurança (@HasPermission)
     * - Regras de multitenancy (TenantPersistenceEntityBase)
     *
     * @param namespace pacote base do projeto
     * @return regras configuradas
     */
    public static ArchbaseArchitectureRules multitenantRestApi(String namespace) {
        return ArchbaseArchitectureRules.forNamespace(namespace)
                .withDddRules()
                .withSpringRules()
                .withNamingRules()
                .withSecurityRules()
                .withMultitenancyRules()
                .checkAll(true);
    }

    /**
     * Profile para API REST sem multitenancy.
     * <p>
     * Inclui:
     * - Regras DDD
     * - Regras Spring
     * - Regras de nomenclatura
     * - Regras de segurança
     *
     * @param namespace pacote base do projeto
     * @return regras configuradas
     */
    public static ArchbaseArchitectureRules simpleRestApi(String namespace) {
        return ArchbaseArchitectureRules.forNamespace(namespace)
                .withDddRules()
                .withSpringRules()
                .withNamingRules()
                .withSecurityRules()
                .checkAll(true);
    }

    /**
     * Profile para microservice simples sem segurança complexa.
     * <p>
     * Inclui:
     * - Regras DDD
     * - Regras Spring
     * - Regras de nomenclatura
     *
     * @param namespace pacote base do projeto
     * @return regras configuradas
     */
    public static ArchbaseArchitectureRules simpleService(String namespace) {
        return ArchbaseArchitectureRules.forNamespace(namespace)
                .withDddRules()
                .withSpringRules()
                .withNamingRules()
                .checkAll(true);
    }

    /**
     * Profile básico com apenas regras DDD.
     * <p>
     * Ideal para bibliotecas ou módulos de domínio.
     *
     * @param namespace pacote base do projeto
     * @return regras configuradas
     */
    public static ArchbaseArchitectureRules domainOnly(String namespace) {
        return ArchbaseArchitectureRules.forNamespace(namespace)
                .withDddRules()
                .withNamingRules()
                .checkAll(true);
    }

    /**
     * Profile completo com todas as regras habilitadas.
     * <p>
     * Recomendado para projetos que seguem estritamente todos os padrões Archbase.
     *
     * @param namespace pacote base do projeto
     * @return regras configuradas
     */
    public static ArchbaseArchitectureRules strict(String namespace) {
        return ArchbaseArchitectureRules.forNamespace(namespace)
                .withAllRules()
                .checkAll(true);
    }

    /**
     * Profile leniente para projetos em migração.
     * <p>
     * Inclui apenas regras básicas:
     * - Regras de nomenclatura
     * - Ciclos de dependência
     *
     * @param namespace pacote base do projeto
     * @return regras configuradas
     */
    public static ArchbaseArchitectureRules lenient(String namespace) {
        return ArchbaseArchitectureRules.forNamespace(namespace)
                .withNamingRules()
                .failOnEmpty(false)
                .checkAll(true);
    }
}
