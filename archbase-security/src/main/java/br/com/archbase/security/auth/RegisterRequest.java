package br.com.archbase.security.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request para registro de novo usuário com suporte a dados adicionais.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição para registro de novo usuário")
public class RegisterRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    @Schema(description = "Nome completo do usuário", example = "João Silva", required = true)
    private String name;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Schema(description = "Email do usuário", example = "joao@exemplo.com", required = true)
    private String email;
    
    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha do usuário", required = true)
    private String password;
    
    @Schema(description = "Role do usuário", example = "USER", defaultValue = "USER")
    private RoleUser role;
    
    @Schema(description = "Avatar do usuário em bytes")
    private byte[] avatar;
    
    @Schema(description = "Dados adicionais específicos da aplicação", 
            example = "{\"phone\": \"+5511999999999\", \"storeId\": \"123\"}")
    private Map<String, Object> additionalData;
}