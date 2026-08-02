package br.com.archbase.security.access;

import br.com.archbase.security.persistence.UserEntity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Quem está pedindo acesso, já resolvido: identidade, origens de permissão e flags.
 *
 * <p><b>Record imutável, desacoplado das entidades JPA de propósito.</b> Montá-lo a partir de
 * associações lazy fora de transação é {@code LazyInitializationException} — defeito que já
 * apareceu no logout durante a auditoria. Resolvendo tudo na construção, o sujeito pode ser
 * carregado uma vez e avaliado em qualquer lugar.
 *
 * <p>É também o que torna a simulação viável sem código paralelo: simular o acesso de outra pessoa
 * é chamar o mesmo {@link ArchbaseAccessEvaluator} com outro sujeito. O carregamento a partir de um
 * {@code userId} — em vez de um {@code Authentication} — chega na fase de diagnóstico.
 *
 * @param administrator {@code Boolean} e não {@code boolean}: a coluna aceita nulo, e nulo não é o
 *                      mesmo que {@code false}. Ver {@link AccessReasonCodes#PRINCIPAL_INCOMPLETE}.
 * @param securityIds   união de usuário, grupos e perfil — as origens que o catálogo consulta
 * @param principal     a entidade de origem, mantida <b>apenas</b> para alimentar SPIs que
 *                      antecedem o core e recebem {@code UserEntity} — {@code ArchbaseRoleResolver}
 *                      é o caso. Nada dentro do core a consulta; ler dela reintroduz o risco de
 *                      lazy loading que o record existe para evitar. Pode ser {@code null}.
 */
public record AccessSubject(
        String userId,
        String userName,
        String email,
        Boolean administrator,
        boolean enabled,
        String profileId,
        String profileName,
        Set<String> groupIds,
        Set<String> securityIds,
        UserEntity principal) {

    public AccessSubject {
        groupIds = groupIds == null ? Set.of() : Set.copyOf(groupIds);
        securityIds = securityIds == null ? Set.of() : Set.copyOf(securityIds);
    }

    /**
     * Constrói o sujeito a partir do usuário autenticado.
     *
     * <p>Toca {@code getGroups()} e {@code getProfile()}, que são lazy — exatamente como
     * {@code collectSecurityIds} já fazia. Chamar dentro do escopo em que a entidade foi carregada
     * continua sendo responsabilidade de quem chama.
     */
    public static AccessSubject of(UserEntity user) {
        if (user == null) {
            return null;
        }

        Set<String> grupos = new LinkedHashSet<>();
        if (user.getGroups() != null) {
            user.getGroups().stream()
                    .filter(ug -> ug.getGroup() != null && ug.getGroup().getId() != null)
                    .map(ug -> ug.getGroup().getId())
                    .forEach(grupos::add);
        }

        String profileId = user.getProfile() == null ? null : user.getProfile().getId();
        String profileName = user.getProfile() == null ? null : user.getProfile().getName();

        // Mesmo conjunto que collectSecurityIds montava: usuário ∪ grupos ∪ perfil, sem precedência.
        Set<String> ids = new HashSet<>();
        if (user.getId() != null) {
            ids.add(user.getId());
        }
        ids.addAll(grupos);
        if (profileId != null) {
            ids.add(profileId);
        }

        return new AccessSubject(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getIsAdministrator(),
                user.isEnabled(),
                profileId,
                profileName,
                grupos,
                ids,
                user);
    }

    /** {@code true} apenas quando a flag está explicitamente marcada. Nulo não é administrador. */
    public boolean isAdministrator() {
        return Boolean.TRUE.equals(administrator);
    }

    /** Como identificar este sujeito em log e em mensagem de diagnóstico. */
    public String label() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        return String.valueOf(userId);
    }
}
