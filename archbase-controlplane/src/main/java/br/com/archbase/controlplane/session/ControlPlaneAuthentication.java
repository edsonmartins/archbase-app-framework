package br.com.archbase.controlplane.session;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Autenticação de uma requisição feita com token do Control Plane.
 * <p>
 * O principal é o {@code UserEntity} local quando o {@code sub} foi resolvido para um usuário do produto — é o que
 * mantém {@code @HasPermission} funcionando, porque o núcleo de autorização só reconhece {@code UserEntity}. Sem
 * usuário local (ou no token de instância), o principal é a própria {@link TenantSession} e {@code @HasPermission}
 * nega; as anotações do Control Plane ({@code @RequiresAdmin} etc.) continuam valendo.
 *
 * @since 3.4.6
 */
public class ControlPlaneAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final transient Object principal;
    private final TenantSession session;

    public ControlPlaneAuthentication(Object principal, TenantSession session,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.session = session;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public TenantSession getSession() {
        return session;
    }

    @Override
    public String getName() {
        if (principal == session) {
            return session.email() != null ? session.email() : session.userId();
        }
        return super.getName();
    }
}
