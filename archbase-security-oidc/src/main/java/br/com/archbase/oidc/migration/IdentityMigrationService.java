package br.com.archbase.oidc.migration;

import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.user.ExternalIdentityStore;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migração da origem da identidade para um provedor OIDC, por tenant, sem recadastrar ninguém (proposta §9.1).
 * <p>
 * O {@code sub} de um usuário no IdP de destino só existe depois que ele é criado lá — então a entrada é sempre um
 * mapeamento vindo do destino: {@code (subject, e-mail ou userId)}. Vale para os dois casos, login local → IdP e
 * IdP → IdP: o {@code sub} do IdP antigo não serve no novo.
 * <ul>
 * <li>{@link #export}: os usuários do tenant, para importar no IdP de destino (senha não migra).</li>
 * <li>{@link #plan}: dry-run — o que seria gravado e por que cada linha seria ignorada; o {@code planHash}
 * identifica esse resultado.</li>
 * <li>{@link #execute}: refaz o plano e só grava se o hash for o mesmo do relatório conferido. Tudo ou nada.</li>
 * </ul>
 * Nada aqui tira acesso: só cria vínculos. O corte (senha local desligada, provedor antigo removido) continua sendo
 * configuração — o plano mostra quem ficaria sem acesso se ele fosse feito agora.
 *
 * @since 3.5.0
 */
public class IdentityMigrationService {

    public static final int MAX_ENTRIES = 10_000;

    private static final Logger log = LoggerFactory.getLogger(IdentityMigrationService.class);

    public record Entry(String subject, String email, String userId) {
    }

    /**
     * @param includeAdministrators vincula também administradores. Desligado por padrão: com
     *                              {@code local-password: disable-on-link}, o administrador vinculado perde a senha
     *                              local — e com ela o acesso de emergência se o IdP cair.
     */
    public record Request(String providerId, List<Entry> entries, boolean includeAdministrators, String planHash) {
    }

    public enum Action { LINK, ALREADY_LINKED, SKIP }

    public enum Reason {
        INVALID_ENTRY, USER_NOT_FOUND, AMBIGUOUS_EMAIL, DUPLICATE_SUBJECT, DUPLICATE_USER,
        SUBJECT_LINKED_TO_OTHER_USER, USER_LINKED_TO_OTHER_SUBJECT, ADMINISTRATOR
    }

    public record Item(int line, String subject, String email, String userId, Action action, Reason reason,
                       boolean administrator, boolean deactivated) {
    }

    public record UserRef(String userId, String name, String email, String userName, boolean administrator,
                          boolean deactivated, String linkedSubject) {
    }

    /**
     * Se o corte fosse feito agora: quantos usuários ativos não teriam como entrar pelo provedor. Administrador sem
     * vínculo é o acesso de emergência — contado à parte.
     */
    public record Cutover(String localPassword, int activeUsersWithoutLink, int administratorsWithoutLink) {
    }

    public record Plan(String providerId, String tenantId, String planHash, Map<String, Integer> summary,
                       List<Item> items, List<UserRef> unmatched, Cutover cutover) {
    }

    /** O plano mudou entre o dry-run e a execução (alguém vinculou, criou ou alterou usuário). */
    public static class PlanChangedException extends RuntimeException {
        private final transient Plan current;

        PlanChangedException(Plan current) {
            super("O plano mudou desde o dry-run");
            this.current = current;
        }

        public Plan current() {
            return current;
        }
    }

    private final UserJpaRepository users;
    private final ExternalIdentityStore links;
    private final IdentityMigrationJournal journal;
    private final ArchbaseOidcProperties properties;
    private final TransactionTemplate transaction;

    public IdentityMigrationService(UserJpaRepository users, ExternalIdentityStore links,
                                    IdentityMigrationJournal journal, ArchbaseOidcProperties properties,
                                    TransactionTemplate transaction) {
        this.users = users;
        this.links = links;
        this.journal = journal;
        this.properties = properties;
        this.transaction = transaction;
    }

    /** Usuários do tenant (o {@code @TenantId} filtra), com o vínculo atual no provedor, se houver. */
    public List<UserRef> export(String providerId, String tenantId) {
        Map<String, String> linked = subjectsByUser(providerId, tenantId);
        return transaction.execute(status -> users.findAll().stream()
                .map(u -> ref(u, linked.get(u.getId())))
                .toList());
    }

    public Plan plan(Request request, String tenantId, String adminId) {
        Plan plan = transaction.execute(status -> build(request, tenantId));
        journal.record(plan.providerId(), tenantId, adminId, IdentityMigrationJournal.Mode.DRY_RUN, plan.planHash(),
                plan.items().size(), 0, plan.summary().getOrDefault(Action.SKIP.name(), 0));
        return plan;
    }

    /** Grava os vínculos do plano, se ele ainda for o conferido. */
    public Plan execute(Request request, String tenantId, String adminId) {
        Plan plan = transaction.execute(status -> {
            Plan current = build(request, tenantId);
            if (request.planHash() == null || !request.planHash().equals(current.planHash())) {
                throw new PlanChangedException(current);
            }
            for (Item item : current.items()) {
                if (item.action() == Action.LINK) {
                    links.link(current.providerId(), tenantId, item.subject(), item.userId());
                }
            }
            return current;
        });
        int linked = plan.summary().getOrDefault(Action.LINK.name(), 0);
        journal.record(plan.providerId(), tenantId, adminId, IdentityMigrationJournal.Mode.EXECUTE, plan.planHash(),
                plan.items().size(), linked, plan.summary().getOrDefault(Action.SKIP.name(), 0));
        log.info("oidc.migration.executed provider={} tenant={} by={} linked={} plan={}", plan.providerId(), tenantId,
                adminId, linked, plan.planHash());
        return plan;
    }

    private Plan build(Request request, String tenantId) {
        String providerId = request.providerId();
        List<UserEntity> all = users.findAll();
        Map<String, UserEntity> byId = all.stream().collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));
        Map<String, List<UserEntity>> byEmail = new HashMap<>();
        for (UserEntity u : all) {
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                byEmail.computeIfAbsent(normalize(u.getEmail()), k -> new ArrayList<>()).add(u);
            }
        }
        Map<String, String> subjectByUser = subjectsByUser(providerId, tenantId);
        Map<String, String> userBySubject = new HashMap<>();
        subjectByUser.forEach((user, subject) -> userBySubject.put(subject, user));

        // 1ª passada: quem é o usuário de cada linha.
        List<Entry> entries = request.entries() == null ? List.of() : request.entries();
        List<Object[]> resolved = new ArrayList<>(); // {entry, user ou Reason}
        Map<String, Integer> subjectCount = new HashMap<>();
        Map<String, Integer> userCount = new HashMap<>();
        for (Entry e : entries) {
            Object target = resolve(e, byId, byEmail);
            resolved.add(new Object[]{e, target});
            if (target instanceof UserEntity u) {
                subjectCount.merge(e.subject().trim(), 1, Integer::sum);
                userCount.merge(u.getId(), 1, Integer::sum);
            }
        }

        // 2ª passada: a ação. Subject ou usuário repetido no arquivo não é desempatado — todas as linhas caem.
        List<Item> items = new ArrayList<>();
        Set<String> coveredUsers = new java.util.HashSet<>();
        int line = 0;
        for (Object[] r : resolved) {
            line++;
            Entry e = (Entry) r[0];
            if (r[1] instanceof Reason reason) {
                items.add(new Item(line, trim(e.subject()), trim(e.email()), trim(e.userId()), Action.SKIP, reason,
                        false, false));
                continue;
            }
            UserEntity u = (UserEntity) r[1];
            String subject = e.subject().trim();
            boolean admin = Boolean.TRUE.equals(u.getIsAdministrator());
            boolean deactivated = Boolean.TRUE.equals(u.getAccountDeactivated());
            Action action = Action.LINK;
            Reason reason = null;
            String linkedUser = userBySubject.get(subject);
            String linkedSubject = subjectByUser.get(u.getId());
            if (subjectCount.get(subject) > 1) {
                action = Action.SKIP;
                reason = Reason.DUPLICATE_SUBJECT;
            } else if (userCount.get(u.getId()) > 1) {
                action = Action.SKIP;
                reason = Reason.DUPLICATE_USER;
            } else if (linkedUser != null && linkedUser.equals(u.getId())) {
                action = Action.ALREADY_LINKED;
            } else if (linkedUser != null) {
                action = Action.SKIP;
                reason = Reason.SUBJECT_LINKED_TO_OTHER_USER;
            } else if (linkedSubject != null) {
                action = Action.SKIP;
                reason = Reason.USER_LINKED_TO_OTHER_SUBJECT;
            } else if (admin && !request.includeAdministrators()) {
                action = Action.SKIP;
                reason = Reason.ADMINISTRATOR;
            }
            if (action != Action.SKIP) {
                coveredUsers.add(u.getId());
            }
            items.add(new Item(line, subject, trim(e.email()), u.getId(), action, reason, admin, deactivated));
        }

        List<UserRef> unmatched = all.stream()
                .filter(u -> !Boolean.TRUE.equals(u.getAccountDeactivated()))
                .filter(u -> !subjectByUser.containsKey(u.getId()) && !coveredUsers.contains(u.getId()))
                .map(u -> ref(u, null))
                .toList();

        Map<String, Integer> summary = new LinkedHashMap<>();
        for (Action a : Action.values()) {
            summary.put(a.name(), 0);
        }
        items.forEach(i -> summary.merge(i.action().name(), 1, Integer::sum));
        items.stream().filter(i -> i.reason() != null).forEach(i -> summary.merge(i.reason().name(), 1, Integer::sum));

        Cutover cutover = new Cutover(
                properties.getProviders().get(providerId).getUsers().getLocalPassword().name(),
                unmatched.size(),
                (int) unmatched.stream().filter(UserRef::administrator).count());

        return new Plan(providerId, tenantId, hash(providerId, tenantId, request.includeAdministrators(), items),
                summary, items, unmatched, cutover);
    }

    private static Object resolve(Entry e, Map<String, UserEntity> byId, Map<String, List<UserEntity>> byEmail) {
        if (e == null || blank(e.subject()) || (blank(e.email()) && blank(e.userId()))) {
            return Reason.INVALID_ENTRY;
        }
        if (!blank(e.userId())) {
            UserEntity u = byId.get(e.userId().trim());
            if (u == null) {
                return Reason.USER_NOT_FOUND;
            }
            // userId e e-mail juntos têm de concordar: divergência é erro de quem montou o arquivo.
            if (!blank(e.email()) && (u.getEmail() == null || !normalize(u.getEmail()).equals(normalize(e.email())))) {
                return Reason.INVALID_ENTRY;
            }
            return u;
        }
        List<UserEntity> found = byEmail.getOrDefault(normalize(e.email()), List.of());
        if (found.isEmpty()) {
            return Reason.USER_NOT_FOUND;
        }
        return found.size() > 1 ? Reason.AMBIGUOUS_EMAIL : found.get(0);
    }

    private Map<String, String> subjectsByUser(String providerId, String tenantId) {
        Map<String, String> map = new HashMap<>();
        for (ExternalIdentityStore.Link l : links.findByProvider(providerId, tenantId)) {
            map.put(l.userId(), l.subject());
        }
        return map;
    }

    private static UserRef ref(UserEntity u, String linkedSubject) {
        return new UserRef(u.getId(), u.getName(), u.getEmail(), u.getUsername(),
                Boolean.TRUE.equals(u.getIsAdministrator()), Boolean.TRUE.equals(u.getAccountDeactivated()),
                linkedSubject);
    }

    /** Identifica o resultado do plano: qualquer linha com outra ação, motivo ou usuário muda o hash. */
    static String hash(String providerId, String tenantId, boolean includeAdministrators, List<Item> items) {
        StringBuilder canonical = new StringBuilder()
                .append(providerId).append('\n').append(tenantId).append('\n').append(includeAdministrators);
        for (Item i : items) {
            canonical.append('\n').append(i.line()).append('|').append(i.subject()).append('|').append(i.userId())
                    .append('|').append(i.action()).append('|').append(i.reason());
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
