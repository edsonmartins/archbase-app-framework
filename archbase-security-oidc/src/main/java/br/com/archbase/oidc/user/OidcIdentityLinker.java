package br.com.archbase.oidc.user;

import br.com.archbase.oidc.auth.OidcIdentity;
import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.LinkByEmail;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.usecase.UserUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Encontra o usuário local de uma identidade do IdP, no tenant já definido no contexto (o filtro o define antes):
 * <ol>
 * <li>pelo vínculo gravado — dali em diante o e-mail pode mudar no IdP sem perder o acesso;</li>
 * <li>sem vínculo, pelo e-mail, se a configuração do provedor permitir — e então grava o vínculo;</li>
 * <li>sem vínculo nem cadastro, com {@code users.provisioning=jit}, cria o usuário — sem grupo, sem perfil, sem senha
 * local e, por padrão, inativo até um administrador aprovar.</li>
 * </ol>
 * Já existindo cadastro com o e-mail, nunca cria outro: ou vincula a ele, ou não entra.
 *
 * @since 3.5.0
 */
public class OidcIdentityLinker {

    private static final Logger log = LoggerFactory.getLogger(OidcIdentityLinker.class);

    private final UserJpaRepository users;
    private final ExternalIdentityStore links;
    private final ArchbaseOidcProperties properties;
    private final UserUseCase userService;

    public OidcIdentityLinker(UserJpaRepository users, ExternalIdentityStore links, ArchbaseOidcProperties properties,
                              UserUseCase userService) {
        this.users = users;
        this.links = links;
        this.properties = properties;
        this.userService = userService;
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

        Optional<UserEntity> byEmail = hasEmail(identity)
                ? users.findByEmailWithGroupsAndProfile(identity.email()) : Optional.empty();
        if (byEmail.isEmpty()) {
            return mayCreate(identity) ? create(identity, tenantId) : Optional.empty();
        }
        // Já existe cadastro com este e-mail: nunca cria outro. Ou vincula a ele, ou não entra.
        if (!mayLinkByEmail(identity)) {
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

    private Optional<UserEntity> create(OidcIdentity identity, String tenantId) {
        Provider provider = properties.getProviders().get(identity.providerId());
        boolean pending = provider.getUsers().getJit().getStatus() == ArchbaseOidcProperties.JitStatus.PENDING;
        String name = identity.name() != null ? identity.name() : identity.email();
        UserEntity novo = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name(name)
                .description("Criado no primeiro acesso pelo provedor " + identity.providerId())
                .email(identity.email())
                .userName(identity.email())
                // Sem senha local e sem poder criar uma por redefinição: a porta de entrada é o IdP.
                .password(null)
                .allowPasswordChange(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .isAdministrator(false)
                .accountDeactivated(pending)
                .accountLocked(false)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .build();
        String userId;
        try {
            userId = userService.createUser(novo.toDto()).getId();
        } catch (RuntimeException e) {
            // Primeiro acesso simultâneo: outro pedido criou o usuário (e talvez o vínculo) antes deste.
            log.info("oidc.user.jit_create_failed provider={} tenant={} cause={}", identity.providerId(), tenantId,
                    e.getMessage());
            return links.findUserId(identity.providerId(), tenantId, identity.subject())
                    .flatMap(users::findByIdWithGroupsAndProfile);
        }
        links.link(identity.providerId(), tenantId, identity.subject(), userId);
        log.info("oidc.user.jit_created provider={} tenant={} userId={} status={}", identity.providerId(), tenantId,
                userId, pending ? "PENDING" : "ACTIVE");
        return users.findByIdWithGroupsAndProfile(userId);
    }

    /** Criar só com e-mail confiável: o e-mail vira a identidade local, e um e-mail alheio viraria personificação. */
    private boolean mayCreate(OidcIdentity identity) {
        Provider provider = properties.getProviders().get(identity.providerId());
        if (provider == null || provider.getUsers().getProvisioning() != ArchbaseOidcProperties.Provisioning.JIT
                || provider.getTenant().isHeader() || !hasEmail(identity)) {
            return false;
        }
        return identity.emailVerified() || provider.getUsers().getLinkByEmail() == LinkByEmail.TRUSTED_PROVIDER;
    }

    private static boolean hasEmail(OidcIdentity identity) {
        return identity.email() != null && !identity.email().isBlank();
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
