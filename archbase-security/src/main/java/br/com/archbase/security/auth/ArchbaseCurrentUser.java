package br.com.archbase.security.auth;

import br.com.archbase.security.persistence.UserEntity;

import java.security.Principal;
import java.util.Optional;

import org.springframework.security.core.Authentication;

/**
 * O usuário local ({@link UserEntity}) de uma autenticação, qualquer que seja o tipo dela.
 * <p>
 * Substitui o cast direto {@code (UserEntity) ((UsernamePasswordAuthenticationToken) principal).getPrincipal()}, que
 * só funcionava para o login local e o token de API e lançava {@link ClassCastException} para qualquer autenticação
 * externa (Control Plane, IdP OIDC). Para o login local e o token de API o resultado é exatamente o mesmo de antes.
 *
 * @since 3.5.0
 */
public final class ArchbaseCurrentUser {

    private ArchbaseCurrentUser() {
    }

    /**
     * O {@link UserEntity} de {@code principal}, se ele for uma {@link Authentication} cujo principal é um usuário
     * local. Vazio para {@code null}, para autenticação sem usuário local (ex.: sessão do Control Plane sem vínculo) e
     * para qualquer outro tipo de principal.
     */
    public static Optional<UserEntity> of(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserEntity user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
