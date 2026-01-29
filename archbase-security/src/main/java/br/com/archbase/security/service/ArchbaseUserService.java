package br.com.archbase.security.service;

import br.com.archbase.security.auth.ChangePasswordRequest;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArchbaseUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository repository;
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {

        var user = (UserEntity) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        // verifique se a senha atual está correta
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalStateException("Senha incorreta");
        }
        // verifique se as duas novas senhas são iguais
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalStateException("Senhas não conferem");
        }

        // atualiza a senha
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // salva usuário
        repository.save(user);
    }

    public void changePassword(ChangePasswordRequest request, User connectedUser) {

        Optional<UserEntity> userEntity = repository.findById(connectedUser.getId().toString());
        if (userEntity.isEmpty()){
            throw new IllegalStateException("Usuário não encontrado");
        }

        // verifique se a senha atual está correta
        if (!passwordEncoder.matches(request.getCurrentPassword(), userEntity.get().getPassword())) {
            throw new IllegalStateException("Senha incorreta");
        }
        // verifique se as duas novas senhas são iguais
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalStateException("Senhas não conferem");
        }

        // atualiza a senha
        userEntity.get().setPassword(passwordEncoder.encode(request.getNewPassword()));

        // salva usuário
        repository.save(userEntity.get());
    }
    
    /**
     * Busca usuário por email.
     * 
     * @param email Email do usuário
     * @return Usuário encontrado
     * @throws IllegalStateException se usuário não for encontrado
     */
    public User findByEmail(String email) {
        Optional<UserEntity> userEntity = repository.findByEmail(email);
        if (userEntity.isEmpty()) {
            throw new IllegalStateException("Usuário não encontrado com email: " + email);
        }
        return userEntity.get().toDomain();
    }
    
    /**
     * Verifica se existe usuário com o email.
     *
     * @param email Email a verificar
     * @return true se existe
     */
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    /**
     * Verifica se existe usuário com o email IGNORANDO o filtro de tenant.
     * Usado para descobrir tenants disponíveis para um email antes do login.
     *
     * @param email Email a verificar
     * @return true se existe algum usuário com esse email em qualquer tenant
     */
    public boolean existsByEmailIgnoringTenant(String email) {
        return repository.existsByEmailIgnoringTenant(email);
    }
}
