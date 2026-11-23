package br.com.archbase.security.exception;

import br.com.archbase.validation.exception.ArchbaseValidationException;

/**
 * O e-mail existe em mais de um tenant e o login não disse qual: o cliente precisa mostrar o seletor de tenant
 * ({@code GET /api/v1/auth/tenants}) e repetir o login com {@code tenantId}.
 * <p>
 * Subclasse de {@link ArchbaseValidationException}, com a mesma mensagem de antes, para quem já tratava a classe-mãe.
 * Existe para que os endpoints de login respondam {@code 400 TENANT_SELECTION_REQUIRED} em vez de cair no tratamento
 * genérico (era 500 em {@code /login} e em {@code /authenticate}).
 *
 * @since 3.5.0
 */
public class ArchbaseTenantSelectionRequiredException extends ArchbaseValidationException {

    public static final String CODE = "TENANT_SELECTION_REQUIRED";

    public ArchbaseTenantSelectionRequiredException() {
        super("Selecione o tenant para efetuar login");
    }
}
