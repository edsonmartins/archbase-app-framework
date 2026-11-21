package br.com.archbase.oidc.user;

import br.com.archbase.oidc.auth.OidcIdentity;
import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.RoleSync;
import br.com.archbase.oidc.token.ClaimReader;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.UserGroupEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aplica {@code role-mapping}: os papéis que o token traz viram participação em grupos locais.
 * <p>
 * A comparação é feita em memória, com os grupos que o usuário já carrega para a autorização; o banco só é tocado
 * quando há diferença — na maioria das requisições, nenhuma escrita. Os cinco portões continuam lendo só o banco: o
 * token muda quem está em qual grupo, nunca concede permissão direta e nunca torna ninguém administrador.
 *
 * @since 3.5.0
 */
public class OidcRoleSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(OidcRoleSynchronizer.class);

    private final ArchbaseOidcProperties properties;
    private final UserJpaRepository users;
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;

    public OidcRoleSynchronizer(ArchbaseOidcProperties properties, UserJpaRepository users,
                                EntityManager entityManager, TransactionTemplate transaction) {
        this.properties = properties;
        this.users = users;
        this.entityManager = entityManager;
        this.transaction = transaction;
    }

    /** O usuário com os grupos já sincronizados (o mesmo objeto, se não havia nada a mudar). */
    public UserEntity sync(UserEntity user, OidcIdentity identity) {
        Provider provider = properties.getProviders().get(identity.providerId());
        if (provider == null || provider.getRoleMapping().getSync() == RoleSync.OFF) {
            return user;
        }
        // Claim de papéis AUSENTE não é "nenhum papel": o Entra omite o groups quando o usuário tem grupos demais
        // (overage), e alguns IdPs só emitem o claim sob um escopo. Tratar ausência como lista vazia faria o
        // mirror apagar os grupos da pessoa a cada login. Só uma lista presente — inclusive vazia — decide.
        if (provider.getClaims().getRoles().stream().allMatch(path -> ClaimReader.read(identity.claims(), path) == null)) {
            return user;
        }
        Map<String, String> map = provider.getRoleMapping().getMap();
        Set<String> desired = externalRoles(identity.claims(), provider.getClaims().getRoles()).stream()
                .map(map::get).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> managed = new HashSet<>(map.values());
        Set<String> current = groupNames(user);

        Set<String> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);
        Set<String> toRemove = new LinkedHashSet<>();
        if (provider.getRoleMapping().getSync() == RoleSync.MIRROR) {
            for (String name : current) {
                if (managed.contains(name) && !desired.contains(name)) {
                    toRemove.add(name);
                }
            }
        }
        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            return user;
        }
        return transaction.execute(status -> apply(user.getId(), toAdd, toRemove, identity.providerId()));
    }

    private UserEntity apply(String userId, Set<String> toAdd, Set<String> toRemove, String providerId) {
        UserEntity user = users.findByIdWithGroupsAndProfile(userId).orElseThrow();
        Set<String> added = new LinkedHashSet<>();
        if (!toAdd.isEmpty()) {
            List<GroupEntity> groups = entityManager
                    .createQuery("SELECT g FROM GroupEntity g WHERE g.name IN :names", GroupEntity.class)
                    .setParameter("names", toAdd)
                    .getResultList();
            Set<String> current = groupNames(user);
            for (GroupEntity group : groups) {
                if (current.add(group.getName())) {
                    user.getGroups().add(UserGroupEntity.builder()
                            .id(UUID.randomUUID().toString())
                            .createEntityDate(LocalDateTime.now())
                            .createdByUser("oidc:" + providerId)
                            .user(user)
                            .group(group)
                            .build());
                    added.add(group.getName());
                }
            }
            Set<String> missing = new LinkedHashSet<>(toAdd);
            missing.removeAll(groups.stream().map(GroupEntity::getName).collect(Collectors.toSet()));
            if (!missing.isEmpty()) {
                log.warn("oidc.roles.group_missing provider={} groups={} — grupo mapeado não existe neste tenant",
                        providerId, missing);
            }
        }
        boolean removed = user.getGroups().removeIf(ug -> ug.getGroup() != null
                && toRemove.contains(ug.getGroup().getName()));
        if (added.isEmpty() && !removed) {
            return user;
        }
        UserEntity saved = users.save(user);
        log.info("oidc.roles.synced provider={} userId={} added={} removed={}", providerId, userId, added,
                removed ? toRemove : Set.of());
        return users.findByIdWithGroupsAndProfile(saved.getId()).orElse(saved);
    }

    private static Set<String> groupNames(UserEntity user) {
        Set<String> names = new LinkedHashSet<>();
        if (user.getGroups() != null) {
            for (UserGroupEntity ug : user.getGroups()) {
                if (ug.getGroup() != null && ug.getGroup().getName() != null) {
                    names.add(ug.getGroup().getName());
                }
            }
        }
        return names;
    }

    /** Papéis de todos os claims configurados: texto, lista de texto ou lista de objetos com {@code name}. */
    static Set<String> externalRoles(Map<String, Object> claims, List<String> paths) {
        Set<String> roles = new LinkedHashSet<>();
        if (paths == null) {
            return roles;
        }
        for (String path : paths) {
            collect(ClaimReader.read(claims, path), roles);
        }
        return roles;
    }

    private static void collect(Object value, Set<String> roles) {
        if (value instanceof String s && !s.isBlank()) {
            roles.add(s);
        } else if (value instanceof Collection<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Object name = m.get("name");
                    if (name instanceof String s && !s.isBlank()) {
                        roles.add(s);
                    }
                } else {
                    collect(item, roles);
                }
            }
        }
    }
}
