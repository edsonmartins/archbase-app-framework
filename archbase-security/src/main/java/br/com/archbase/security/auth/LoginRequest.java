package br.com.archbase.security.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para login flexível com suporte a email ou telefone.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição para login flexível")
public class LoginRequest {
    
    @NotBlank(message = "Identificador é obrigatório")
    @Schema(description = "Email ou telefone do usuário", 
            example = "usuario@exemplo.com ou 11999999999", 
            required = true)
    private String identifier;
    
    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha do usuário", required = true)
    private String password;
    
    @Schema(description = "Contexto da aplicação", 
            example = "STORE_APP", 
            defaultValue = "DEFAULT")
    private String context;
}