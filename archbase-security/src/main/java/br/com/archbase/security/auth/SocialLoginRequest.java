package br.com.archbase.security.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para login via provedor social (Google, Facebook, etc).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição para login social")
public class SocialLoginRequest {
    
    @NotBlank(message = "Provedor é obrigatório")
    @Schema(description = "Nome do provedor social", 
            example = "google", 
            allowableValues = {"google", "facebook", "apple"},
            required = true)
    private String provider;
    
    @NotBlank(message = "Token é obrigatório")
    @Schema(description = "Token de autenticação do provedor", required = true)
    private String token;
    
    @Schema(description = "Contexto da aplicação", 
            example = "CUSTOMER_APP", 
            defaultValue = "DEFAULT")
    private String context;
    
    @Schema(description = "Dados adicionais do usuário fornecidos pelo app", 
            example = "{\"phone\": \"+5511999999999\"}")
    private String additionalData;
}