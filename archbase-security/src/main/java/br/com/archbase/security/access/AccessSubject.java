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
 * @param administrator {@code Boolean} e não {@code boolean}: {@code BO_ADMINISTRADOR} é nulável, e
 *                      nulo não é o mesmo que {@code false} — embora seja tratado como tal na
 *                      decisão, porque nulo jamais pode significar "é administrador"
 * @param groupNames    nomes dos grupos, para diagnóstico. O relatório de efetivo existe para
 *                      evitar abrir grupo por grupo no admin; exibir identificadores no lugar dos
 *                      nomes devolveria o problema a quem lê
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
        Set<String> groupNames,
        Set<String> securityIds,
        AccessLevel level,
        UserEntity principal) {

    public AccessSubject {
        groupIds = groupIds == null ? Set.of() : Set.copyOf(groupIds);
        groupNames = groupNames == null ? Set.of() : Set.copyOf(groupNames);
        securityIds = securityIds == null ? Set.of() : Set.copyOf(securityIds);
    }

    /**
     * Constrói o sujeito a partir do usuário autenticado.
     *
     * <p><b>Inicializa o perfil.</b> O {@code collectSecurityIds} anterior lia apenas
     * {@code getProfile().getId()}, que um proxy do Hibernate responde sem ir ao banco. Aqui são
     * lidos também o nome e o nível, o que força a inicialização — uma consulta a mais por
     * avaliação, para usuários que tenham perfil. É o custo de o sujeito carregar o que a decisão
     * e o diagnóstico precisam; quem chama continua responsável por fazê-lo dentro do escopo em
     * que a entidade foi carregada, ou usar {@code ArchbaseAccessSubjectLoader}, que resolve tudo
     * com fetch join.
     */
    public static AccessSubject of(UserEntity user) {
        if (user == null) {
            return null;
        }

        Set<String> grupos = new LinkedHashSet<>();
        Set<String> nomesDosGrupos = new LinkedHashSet<>();
        if (user.getGroups() != null) {
            user.getGroups().stream()
                    .filter(ug -> ug.getGroup() != null && ug.getGroup().getId() != null)
                    .forEach(ug -> {
                        grupos.add(ug.getGroup().getId());
                        if (ug.getGroup().getName() != null) {
                            nomesDosGrupos.add(ug.getGroup().getName());
                        }
                    });
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
                nomesDosGrupos,
                ids,
                user.getProfile() == null ? null : user.getProfile().getAccessLevel(),
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
