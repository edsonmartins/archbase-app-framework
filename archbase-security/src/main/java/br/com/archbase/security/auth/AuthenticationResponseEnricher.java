package br.com.archbase.security.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface para enriquecimento customizado da resposta de autenticação.
 * Permite que aplicações implementem lógica específica para adicionar dados
 * contextuais à resposta de login (ex: dados da loja, configurações específicas).
 * 
 * Implementações desta interface são automaticamente detectadas pelo Archbase
 * via Spring Boot auto-configuration e aplicadas durante o processo de autenticação.
 */
public interface AuthenticationResponseEnricher {
    
    /**
     * Enriquece a resposta de autenticação com dados específicos da aplicação.
     *
     * @param baseResponse Resposta básica gerada pelo Archbase (token + user básico)
     * @param context Contexto da aplicação (ex: "STORE_APP", "CUSTOMER_APP", "WEB_ADMIN")
     * @param request Request HTTP original para contexto adicional (headers, IP, etc.)
     * @return Resposta enriquecida com dados específicos da aplicação
     */
    AuthenticationResponse enrich(AuthenticationResponse baseResponse, 
                                String context, 
                                HttpServletRequest request);
    
    /**
     * Indica se este enricher deve ser aplicado para o contexto especificado.
     * Permite que enrichers específicos sejam aplicados apenas para certos contextos.
     *
     * @param context Contexto da aplicação
     * @return true se deve ser aplicado, false caso contrário
     */
    default boolean supports(String context) {
        return true;
    }
    
    /**
     * Ordem de execução quando múltiplos enrichers estão registrados.
     * Enrichers são executados em ordem crescente de prioridade.
     * Menor valor = maior prioridade (executado primeiro).
     *
     * @return ordem de execução (padrão: 0)
     */
    default int getOrder() {
        return 0;
    }
}