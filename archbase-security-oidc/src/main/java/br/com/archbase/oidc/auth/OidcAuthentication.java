package br.com.archbase.oidc.auth;

import br.com.archbase.security.persistence.UserEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Requisição autenticada por token de IdP OIDC.
 * <p>
 * O principal é sempre o {@link UserEntity} vinculado: é o que o núcleo de autorização reconhece, então
 * {@code @HasPermission} decide pelas permissões locais do usuário, como no login próprio. A identidade do IdP fica
 * em {@link #getIdentity()}.
 *
 * @since 3.5.0
 */
public class OidcAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final transient UserEntity user;
    private final transient OidcIdentity identity;

    public OidcAuthentication(UserEntity user, OidcIdentity identity) {
        super(user.getAuthorities());
        this.user = user;
        this.identity = identity;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }

    public OidcIdentity getIdentity() {
        return identity;
    }

    @Override
    public String getName() {
        return user.getEmail() != null ? user.getEmail() : user.getId();
    }
}
