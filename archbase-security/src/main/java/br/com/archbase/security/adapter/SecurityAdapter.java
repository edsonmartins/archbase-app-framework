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
        if (!authentication.isAuthenticated()) {
            throw new ArchbaseValidationException("Usuário não autenticado.");
        }
        UserEntity principal = (UserEntity) authentication.getPrincipal();
        Optional<UserEntity> byId = userJpaRepository.findById(principal.getId());
        return byId.get().toDomain();
    }

    @Override
    public Principal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.isAuthenticated()) {
            throw new ArchbaseValidationException("Usuário não autenticado.");
        }
        return (Principal) authentication.getPrincipal();
    }


}
