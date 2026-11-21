package br.com.archbase.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Corpo do {@code PATCH /api/v1/user}.
 * <p>
 * O construtor vazio é obrigatório: só com {@code @Builder} o Lombok gera um construtor package-private com todos
 * os campos, o Jackson não consegue montar o objeto e o endpoint respondia 500 ("Type definition error") a toda
 * troca de senha.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    private String currentPassword;
    private String newPassword;
    private String confirmationPassword;
}
