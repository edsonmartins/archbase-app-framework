package br.com.archbase.oidc.user;

import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.LocalPassword;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.spi.ArchbaseLocalCredentialPolicy;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Desliga a senha local de quem está vinculado a um provedor com {@code users.local-password=disable-on-link}.
 * Provedores com {@code keep} (padrão) não afetam a senha local.
 *
 * @since 3.5.0
 */
public class OidcLocalCredentialPolicy implements ArchbaseLocalCredentialPolicy {

    private final ExternalIdentityStore links;
    private final Set<String> disablingProviders;

    public OidcLocalCredentialPolicy(ExternalIdentityStore links, ArchbaseOidcProperties properties) {
        this.links = links;
        this.disablingProviders = properties.getProviders().entrySet().stream()
                .filter(e -> e.getValue().getUsers().getLocalPassword() == LocalPassword.DISABLE_ON_LINK)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean allowsLocalPassword(UserEntity user) {
        return disablingProviders.isEmpty() || !links.isLinked(user.getId(), disablingProviders);
    }
}
