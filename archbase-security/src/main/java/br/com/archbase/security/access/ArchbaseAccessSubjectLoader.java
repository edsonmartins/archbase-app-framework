package br.com.archbase.security.access;

import br.com.archbase.security.repository.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Carrega um {@link AccessSubject} a partir de um identificador, e não de um
 * {@code Authentication}.
 *
 * <p><b>É o que torna a simulação viável.</b> Perguntar "o usuário X conseguiria fazer Y?" passa a
 * ser chamar o mesmo {@link ArchbaseAccessEvaluator} com outro sujeito — sem código paralelo, e
 * portanto sem o risco de a simulação divergir da decisão real, que é o defeito clássico desse tipo
 * de ferramenta.
 *
 * <p>Usa consulta com fetch join de grupos e perfil: o sujeito é montado fora do escopo
 * transacional de uma requisição autenticada, e tocar associação lazy ali é
 * {@code LazyInitializationException}.
 */
@Component
public class ArchbaseAccessSubjectLoader {

    private final UserJpaRepository userRepository;

    public ArchbaseAccessSubjectLoader(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<AccessSubject> byId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByIdWithGroupsAndProfile(userId).map(AccessSubject::of);
    }

    @Transactional(readOnly = true)
    public Optional<AccessSubject> byEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailWithGroupsAndProfile(email).map(AccessSubject::of);
    }

    /**
     * Resolve por id ou, na falta dele, por e-mail — a ordem em que quem opera o admin costuma ter
     * o dado em mãos.
     */
    @Transactional(readOnly = true)
    public Optional<AccessSubject> resolve(String userId, String email) {
        Optional<AccessSubject> porId = byId(userId);
        return porId.isPresent() ? porId : byEmail(email);
    }
}
