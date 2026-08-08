package br.com.archbase.security.auditing;

import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Quem está gravando, para as colunas de autoria de {@code PersistenceEntityBase}.
 *
 * <p><b>O cast que estava aqui nunca poderia dar certo.</b> A versão anterior fazia
 * {@code (User) authentication.getPrincipal()}, mas o principal que o
 * {@code ArchbaseJwtAuthenticationFilter} coloca no contexto é um {@link UserEntity} — que estende
 * {@code SecurityEntity} e implementa {@link UserDetails}, e <b>não é</b> um
 * {@link User}. Toda escrita autenticada teria terminado em {@code ClassCastException}.
 *
 * <p>Não terminava porque o listener de auditoria nunca era acionado: faltava o
 * {@code @EntityListeners} na entidade base. O defeito ficou escondido atrás do outro, e só
 * apareceria no dia em que alguém corrigisse o primeiro.
 *
 * <p>Agora cada forma de principal é tratada pelo que ela é, e o que não for reconhecido devolve
 * vazio em vez de derrubar a gravação. Auditoria que não sabe quem gravou é um campo em branco;
 * auditoria que estoura é uma operação de negócio perdida — e a segunda é muito pior que a primeira.
 */
public class ApplicationAuditAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        // O caminho normal: é o que o filtro JWT do próprio framework coloca no contexto.
        if (principal instanceof UserEntity user) {
            return Optional.ofNullable(user.getId());
        }

        // Aplicações que montam o principal a partir do domínio.
        if (principal instanceof User user) {
            return Optional.ofNullable(user.getId()).map(Object::toString);
        }

        // Aplicação com UserDetailsService próprio — suportado oficialmente
        // (o bean do framework é @ConditionalOnMissingBean). Não dá para saber o id, e o login é o
        // identificador que ela tem; melhor gravar isso do que gravar nada.
        if (principal instanceof UserDetails details) {
            return Optional.ofNullable(details.getUsername());
        }

        // Token de API, autenticação de serviço, principal em String: o nome é o que há.
        String nome = authentication.getName();
        return (nome == null || nome.isBlank()) ? Optional.empty() : Optional.of(nome);
    }
}
