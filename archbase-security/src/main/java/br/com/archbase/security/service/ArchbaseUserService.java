package br.com.archbase.security.service;

import br.com.archbase.security.auth.ChangePasswordRequest;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;

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
}
