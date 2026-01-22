package br.com.archbase.security.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request de autenticação que inclui contexto da aplicação.
 * Permite que o sistema identifique qual tipo de aplicação está fazendo login
 * e customize a resposta adequadamente através de enrichers.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Requisição de autenticação com contexto específico da aplicação")
public class ContextualAuthenticationRequest {
    
    /**
     * Email do usuário para autenticação.
     */
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Schema(description = "Email do usuário", example = "usuario@exemplo.com", required = true)
    private String email;
    
    /**
     * Senha do usuário para autenticação.
     */
    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha do usuário", required = true)
    private String password;
    
    /**
     * Contexto da aplicação que está fazendo login.
     * Exemplos: "STORE_APP", "CUSTOMER_APP", "DRIVER_APP", "WEB_ADMIN"
     * 
     * Este valor é usado pelos enrichers para determinar que tipo de dados
     * adicionar à resposta de autenticação.
     */
    @Schema(description = "Contexto da aplicação (STORE_APP, CUSTOMER_APP, DRIVER_APP, WEB_ADMIN)", 
            example = "STORE_APP", 
            defaultValue = "WEB_ADMIN")
    private String context;
    
    /**
     * Informações adicionais específicas do contexto.
     * Pode conter dados como versão do app, plataforma, tenant ID, etc.
     * Formato livre - depende da implementação do enricher.
     */
    @Schema(description = "Dados adicionais do contexto em formato JSON", 
            example = "{\"storeId\": \"123\"}")
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