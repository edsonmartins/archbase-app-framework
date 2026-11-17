package br.com.archbase.controlplane.user;

import br.com.archbase.controlplane.token.ControlPlaneClaims;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Encontra o usuário local do produto para o {@code sub} do token, no tenant já definido no contexto:
 * <ol>
 * <li>pelo vínculo gravado em {@link UserLinkStore};</li>
 * <li>pelo id local igual ao {@code sub} (produto que já usa o id do Control Plane);</li>
 * <li>pelo e-mail do token — e então grava o vínculo, para não depender mais do e-mail.</li>
 * </ol>
 *
 * @since 3.4.6
 */
public class ControlPlaneUserResolver {

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneUserResolver.class);

    private final UserJpaRepository users;
    private final UserLinkStore links;

    public ControlPlaneUserResolver(UserJpaRepository users, UserLinkStore links) {
        this.users = users;
        this.links = links;
    }

    public Optional<UserEntity> resolve(ControlPlaneClaims claims, String tenantId) {
        Optional<String> linked = links.findLocalUserId(tenantId, claims.subject());
        if (linked.isPresent()) {
            Optional<UserEntity> user = users.findByIdWithGroupsAndProfile(linked.get());
            if (user.isPresent()) {
                return user;
            }
            log.warn("controlplane.user.link_orphan tnt={} sub={} userId={} — usuário local não existe mais",
                    tenantId, claims.subject(), linked.get());
        }

        Optional<UserEntity> byId = users.findByIdWithGroupsAndProfile(claims.subject());
        if (byId.isPresent()) {
            return byId;
        }

        if (claims.email() != null && !claims.email().isBlank()) {
            Optional<UserEntity> byEmail = users.findByEmailWithGroupsAndProfile(claims.email());
            byEmail.ifPresent(user -> links.link(tenantId, claims.subject(), user.getId()));
            return byEmail;
        }
        return Optional.empty();
    }
}
