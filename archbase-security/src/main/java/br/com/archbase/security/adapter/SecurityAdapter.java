package br.com.archbase.security.adapter;

import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.usecase.GetSecurityData;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Component
public class SecurityAdapter implements GetSecurityData {

    @Autowired
    UserJpaRepository userJpaRepository;

    @Override
    public User getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ArchbaseValidationException("Usuário não autenticado.");
        }
        UserEntity principal = (UserEntity) authentication.getPrincipal();
        Optional<UserEntity> byId = userJpaRepository.findById(principal.getId());
        return byId.orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado.")).toDomain();
    }

    @Override
    public Principal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ArchbaseValidationException("Usuário não autenticado.");
        }
        return (Principal) authentication.getPrincipal();
    }

    /**
     * Verifica se existe um usuário autenticado no contexto atual.
     *
     * @return true se existe um usuário autenticado
     */
    public boolean hasAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserEntity;
    }

    /**
     * Retorna o usuário logado ou null se não houver usuário autenticado.
     * Útil para cenários onde a autenticação é opcional.
     *
     * @return O usuário logado ou null
     */
    public User getLoggedUserOrNull() {
        if (!hasAuthenticatedUser()) {
            return null;
        }
        return getLoggedUser();
    }
}
