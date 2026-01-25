package br.com.archbase.architecture.rules.test;

import br.com.archbase.architecture.rules.core.ArchbaseArchitectureRules;
import org.junit.jupiter.api.Test;

/**
 * Classe base para testes de arquitetura Archbase.
 * <p>
 * Projetos que utilizam o framework Archbase podem estender esta classe
 * para validar que seguem os padrões arquiteturais definidos.
 * <p>
 * Exemplo de uso:
 * <pre>{@code
 * public class MinhaArquiteturaTest extends ArchbaseArchitectureTest {
 *
 *     @Override
 *     protected String getBasePackage() {
 *         return "com.minhaempresa.meuprojeto";
 *     }
 *
 *     // Opcional: sobrescrever para personalizar regras
 *     @Override
 *     protected boolean enableSecurityRules() {
 *         return true; // habilita validação de @HasPermission
 *     }
 * }
 * }</pre>
 *
 * @author Archbase Team
 * @since 2.0.1
 */
public abstract class ArchbaseArchitectureTest {

    /**
     * Define o pacote base do projeto a ser analisado.
     * <p>
     * Exemplo: "com.minhaempresa.meuprojeto"
     *
     * @return pacote base do projeto
     */
    protected abstract String getBasePackage();

    /**
     * Define se as regras DDD devem ser habilitadas.
     * Padrão: true
     */
    protected boolean enableDddRules() {
        return true;
    }

    /**
     * Define se as regras Spring devem ser habilitadas.
     * Padrão: true
     */
    protected boolean enableSpringRules() {
        return true;
    }

    /**
     * Define se as regras de nomenclatura devem ser habilitadas.
     * Padrão: true
     */
    protected boolean enableNamingRules() {
        return true;
    }

    /**
     * Define se as regras de segurança devem ser habilitadas.
     * Padrão: false (habilitar manualmente quando usar @HasPermission)
     */
    protected boolean enableSecurityRules() {
        return false;
    }

    /**
     * Define se as regras de multitenancy devem ser habilitadas.
     * Padrão: false (habilitar quando usar multitenancy)
     */
    protected boolean enableMultitenancyRules() {
        return false;
    }

    /**
     * Define se as regras de teste devem ser habilitadas.
     * Padrão: false
     */
    protected boolean enableTestRules() {
        return false;
    }

    /**
     * Define se deve falhar quando não encontrar classes.
     * Padrão: true
     */
    protected boolean failOnEmpty() {
        return true;
    }

    /**
     * Define se deve coletar todos os erros antes de falhar.
     * Padrão: true (mostra todos os problemas de uma vez)
     */
    protected boolean checkAll() {
        return true;
    }

    /**
     * Hook para adicionar regras customizadas.
     * Sobrescreva este método para adicionar regras específicas do projeto.
     *
     * @param rules builder de regras
     */
    protected void configureCustomRules(ArchbaseArchitectureRules rules) {
        // Por padrão, não adiciona regras customizadas
    }

    /**
     * Executa a validação das regras de arquitetura.
     */
    @Test
    void shouldFollowArchbaseArchitecturePatterns() {
        ArchbaseArchitectureRules rules = ArchbaseArchitectureRules.forNamespace(getBasePackage())
                .failOnEmpty(failOnEmpty())
                .checkAll(checkAll());

        if (enableDddRules()) {
            rules.withDddRules();
        }

        if (enableSpringRules()) {
            rules.withSpringRules();
        }

        if (enableNamingRules()) {
            rules.withNamingRules();
        }

        if (enableSecurityRules()) {
            rules.withSecurityRules();
        }

        if (enableMultitenancyRules()) {
            rules.withMultitenancyRules();
        }

        if (enableTestRules()) {
            rules.withTestRules();
        }

        // Permite adicionar regras customizadas
        configureCustomRules(rules);

        rules.check();
    }
}
