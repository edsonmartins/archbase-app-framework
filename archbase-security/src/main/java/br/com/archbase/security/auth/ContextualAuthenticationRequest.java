package br.com.archbase.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request de autenticação que inclui contexto da aplicação.
 * Permite que o sistema identifique qual tipo de aplicação está fazendo login
 * e customize a resposta adequadamente através de enrichers.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContextualAuthenticationRequest {
    
    /**
     * Email do usuário para autenticação.
     */
    private String email;
    
    /**
     * Senha do usuário para autenticação.
     */
    private String password;
    
    /**
     * Contexto da aplicação que está fazendo login.
     * Exemplos: "STORE_APP", "CUSTOMER_APP", "DRIVER_APP", "WEB_ADMIN"
     * 
     * Este valor é usado pelos enrichers para determinar que tipo de dados
     * adicionar à resposta de autenticação.
     */
    private String context;
    
    /**
     * Informações adicionais específicas do contexto.
     * Pode conter dados como versão do app, plataforma, tenant ID, etc.
     * Formato livre - depende da implementação do enricher.
     */
    private String contextData;
    
    /**
     * Converte para AuthenticationRequest básico (compatibilidade).
     * Remove dados contextuais e mantém apenas credenciais.
     * 
     * @return AuthenticationRequest básico para autenticação padrão
     */
    public AuthenticationRequest toBasicRequest() {
        return AuthenticationRequest.builder()
                .email(this.email)
                .password(this.password)
                .build();
    }
}