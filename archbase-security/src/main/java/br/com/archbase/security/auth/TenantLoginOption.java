package br.com.archbase.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa um tenant disponível para login de um determinado email.
 * Usado pelo endpoint GET /api/v1/auth/tenants para que o frontend exiba o
 * seletor de tenant quando um mesmo email pertence a múltiplos tenants.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantLoginOption {

    private String tenantId;
    private String nome;
    private String descricao;
}
