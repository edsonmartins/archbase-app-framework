package br.com.archbase.oidc.user;

import br.com.archbase.oidc.auth.OidcIdentity;
import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.LinkByEmail;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Encontra o usuário local de uma identidade do IdP, no tenant já definido no contexto (o filtro o define antes):
 * <ol>
 * <li>pelo vínculo gravado — dali em diante o e-mail pode mudar no IdP sem perder o acesso;</li>
 * <li>sem vínculo, pelo e-mail, se a configuração do provedor permitir — e então grava o vínculo.</li>
 * </ol>
 * Não cria usuário: nesta fase só entra quem já tem cadastro na aplicação ({@code existing-user}).
 *
 * @since 3.5.0
 */
public class OidcIdentityLinker {

    private static final Logger log = LoggerFactory.getLogger(OidcIdentityLinker.class);

    private final UserJpaRepository users;
    private final ExternalIdentityStore links;
    private final ArchbaseOidcProperties properties;

    public OidcIdentityLinker(UserJpaRepository users, ExternalIdentityStore links, ArchbaseOidcProperties properties) {
        this.users = users;
        this.links = links;
        this.properties = properties;
    }

    public Optional<UserEntity> resolve(OidcIdentity identity, String tenantId) {
        Optional<String> linked = links.findUserId(identity.providerId(), tenantId, identity.subject());
        if (linked.isPresent()) {
            Optional<UserEntity> user = users.findByIdWithGroupsAndProfile(linked.get());
            if (user.isPresent()) {
                return user;
            }
            // Vínculo aponta para usuário removido. Não cai no e-mail: quem removeu o usuário não quis o acesso.
            log.warn("oidc.user.link_orphan provider={} tenant={} userId={}", identity.providerId(), tenantId,
                    linked.get());
            return Optional.empty();
        }

        if (!mayLinkByEmail(identity)) {
            return Optional.empty();
        }
        Optional<UserEntity> byEmail = users.findByEmailWithGroupsAndProfile(identity.email());
        if (byEmail.isEmpty()) {
            return Optional.empty();
        }
        UserEntity user = byEmail.get();
        // O usuário já pertence a OUTRA identidade deste provedor: uma segunda conta no IdP com o mesmo e-mail
        // não pode assumi-lo.
        Optional<String> other = links.findSubject(identity.providerId(), tenantId, user.getId());
        if (other.isPresent() && !other.get().equals(identity.subject())) {
            log.warn("oidc.user.link_conflict provider={} tenant={} userId={} — usuário já vinculado a outra "
                    + "identidade do provedor", identity.providerId(), tenantId, user.getId());
            return Optional.empty();
        }
        links.link(identity.providerId(), tenantId, identity.subject(), user.getId());
        return byEmail;
    }

    private boolean mayLinkByEmail(OidcIdentity identity) {
        Provider provider = properties.getProviders().get(identity.providerId());
        if (provider == null || provider.getTenant().isHeader()) {
            // Tenant escolhido pelo cliente: vincular por e-mail deixaria o header escolher a conta.
            return false;
        }
        if (identity.email() == null || identity.email().isBlank()) {
            return false;
        }
        LinkByEmail mode = provider.getUsers().getLinkByEmail();
        return mode == LinkByEmail.TRUSTED_PROVIDER || (mode == LinkByEmail.VERIFIED_ONLY && identity.emailVerified());
    }
}
