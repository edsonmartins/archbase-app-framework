package br.com.archbase.security.service;

import br.com.archbase.security.adapter.AccessTokenPersistenceAdapter;
import br.com.archbase.security.adapter.PasswordResetTokenPersistenceAdapter;
import br.com.archbase.security.auth.*;
import br.com.archbase.security.domain.entity.PasswordResetToken;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.token.TokenType;
import br.com.archbase.security.util.TokenGeneratorUtil;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchbaseAuthenticationService {
    private final UserJpaRepository repository;
    private final AccessTokenJpaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArchbaseJwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ArchbaseEmailService archbaseEmailService;
    private final UserService userService;
    private final PasswordResetTokenPersistenceAdapter passwordResetTokenPersistenceAdapter;
    private final AccessTokenPersistenceAdapter accessTokenPersistenceAdapter;

    public void register(RegisterNewUser request) {
        Optional<UserEntity> byEmail = repository.findByEmail(request.getEmail());
        UserEntity user;
        if (byEmail.isEmpty()) {
            user = UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createEntityDate(LocalDateTime.now())
                    .name(request.getName())
                    .description(request.getDescription())
                    .email(request.getEmail())
                    .userName(request.getUserName())
                    .passwordNeverExpires(true)
                    .allowPasswordChange(true)
                    .avatar(request.getAvatar())
                    .password(request.getPassword())
                    .build();
            userService.createUser(user.toDto());
        } else {
            throw new ArchbaseValidationException("Usuário já existe.");
        }
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            var user = repository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado"));

            // Verificar se existe um token válido
            AccessTokenEntity accessToken = accessTokenPersistenceAdapter.findValidTokenByUser(user);

            // Verificar se o token existe e não está expirado
            if (accessToken != null && !jwtService.isTokenExpired(accessToken.getToken())) {
                log.debug("Token válido encontrado para o usuário {}, reusando token", user.getEmail());
                // Token ainda válido, retorna o mesmo
                var refreshToken = jwtService.generateRefreshToken(user);
                return buildAuthenticationResponse(accessToken, refreshToken.token(), user);
            }

            log.debug("Nenhum token válido encontrado para o usuário {}, criando novo token", user.getEmail());
            // Sempre revogar tokens antigos antes de criar novos
            revokeAllUserTokens(user);

            // Gerar novos tokens
            var jwtToken = jwtService.generateToken(user);
            accessToken = saveUserToken(user, jwtToken);
            var refreshToken = jwtService.generateRefreshToken(user);

            return buildAuthenticationResponse(accessToken, refreshToken.token(), user);
        } catch (AuthenticationException e) {
            log.warn("Falha na autenticação", e);
            throw new BadCredentialsException("Login ou senha inválido", e);
        }
    }

    private AccessTokenEntity saveUserToken(UserEntity usuario, ArchbaseJwtService.TokenResult jwtToken) {
        // Usar UTC para datas de expiração
        LocalDateTime expirationDateTime = convertToLocalDateTimeViaInstant(jwtService.extractExpiration(jwtToken.token()));

        log.debug("Salvando novo token para usuário {} com expiração em {}",
                usuario.getEmail(), expirationDateTime);

        var token = AccessTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .user(usuario)
                .token(jwtToken.token())
                .expirationTime(jwtToken.expiresIn())
                .expirationDate(expirationDateTime)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        return tokenRepository.save(token);
    }

    private LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.of("UTC"))
                .toLocalDateTime();
    }

    public LocalDateTime convertToLocalDateTimeViaMilisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    @Transactional
    public void revokeAllUserTokens(UserEntity user) {
        log.debug("Revogando todos os tokens válidos para o usuário {}", user.getEmail());

        var validUserTokens = accessTokenPersistenceAdapter.findAllValidTokenByUser(user);
        if (!validUserTokens.isEmpty()) {
            log.debug("Encontrados {} tokens válidos para revogação", validUserTokens.size());
            validUserTokens.forEach(token -> {
                token.setExpired(true);
                token.setRevoked(true);
            });
            tokenRepository.saveAll(validUserTokens);
        } else {
            log.debug("Nenhum token válido encontrado para revogação");
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshToken) {
        try {
            String userEmail = jwtService.extractUsername(refreshToken.getToken());
            if (userEmail == null) {
                log.warn("Refresh token inválido: não foi possível extrair o email do usuário");
                throw new JwtException("Token inválido");
            }

            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.warn("Usuário não encontrado para o email: {}", userEmail);
                        return new ArchbaseValidationException("Usuário não encontrado");
                    });

            if (!jwtService.isTokenValid(refreshToken.getToken(), user)) {
                log.warn("Refresh token inválido para o usuário: {}", userEmail);
                throw new JwtException("Token de refresh inválido");
            }

            // Sempre revogar tokens antigos para evitar acumulação
            revokeAllUserTokens(user);

            // Gerar novos tokens
            var jwtToken = jwtService.generateToken(user);
            AccessTokenEntity accessToken = saveUserToken(user, jwtToken);
            var newRefreshToken = jwtService.generateRefreshToken(user);

            log.debug("Token refreshed com sucesso para o usuário: {}", userEmail);
            return buildAuthenticationResponse(accessToken, newRefreshToken.token(), user);

        } catch (JwtException e) {
            log.error("Erro ao processar refresh token", e);
            throw new JwtException("Erro ao processar token: " + e.getMessage());
        }
    }

    // Método auxiliar para construir resposta de autenticação
    private AuthenticationResponse buildAuthenticationResponse(AccessTokenEntity accessToken, String refreshToken, UserEntity user) {
        return AuthenticationResponse.builder()
                .id(accessToken.getId())
                .accessToken(accessToken.getToken())
                .expirationTime(accessToken.getExpirationTime())
                .tokenType(TokenType.BEARER)
                .refreshToken(refreshToken)
                .user(user != null ? user.toDomain() : null)
                .build();
    }


    public void sendResetPasswordEmail(String email)  {
        Optional<UserEntity> usuarioOptional = repository.findByEmail(email);
        if(usuarioOptional.isEmpty()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s  não foi encontrado.",email));
        }
        UserEntity user = usuarioOptional.get();
        revokeExistingTokens(user);
        if (user.getAllowPasswordChange()) {
            String passwordResetToken = createPasswordResetToken(user.toDomain());
            archbaseEmailService.sendResetPasswordEmail(email, passwordResetToken, user.getUsername(), user.getName());
        } else {
            throw new ArchbaseValidationException(String.format("Usuário com email %s  não possui autorização para alterar a senha.",email));
        }
    }

    public String createPasswordResetToken(User user) {
        String passwordResetToken = TokenGeneratorUtil.generateNumericToken();
        PasswordResetToken token = new PasswordResetToken(passwordResetToken, user);
        passwordResetTokenPersistenceAdapter.save(token);
        return passwordResetToken;
    }

    private void revokeExistingTokens(UserEntity user) {
        List<PasswordResetToken> passwordResetTokenList = passwordResetTokenPersistenceAdapter.findAllNonExpiredAndNonRevokedTokens(user);
        if (!passwordResetTokenList.isEmpty()) {
            passwordResetTokenList.forEach(PasswordResetToken::revokeToken);
            passwordResetTokenPersistenceAdapter.saveAll(passwordResetTokenList);
        }
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        Optional<UserEntity> usuarioOptional = repository.findByEmail(request.getEmail());
        if(usuarioOptional.isEmpty()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s não foi encontrado.", request.getEmail()));
        }
        UserEntity user = usuarioOptional.get();

        PasswordResetToken token = passwordResetTokenPersistenceAdapter.findToken(user, request.getPasswordResetToken());

        if (token == null) {
            throw new ArchbaseValidationException("Token de redefinição de senha inválido.");
        }
        token.updateExpired();
        passwordResetTokenPersistenceAdapter.save(token);

        if (token.isExpired()) {
            throw new ArchbaseValidationException("Token de redefinição de senha expirado, favor gerar novamente.");
        }

        if (token.isRevoked()) {
            throw new ArchbaseValidationException("Token de redefinição de senha inválido, favor utilizar o token mais recente.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        repository.save(user);
        token.revokeToken();
        passwordResetTokenPersistenceAdapter.save(token);

        // Revogar tokens de acesso ao alterar a senha
        revokeAllUserTokens(user);
    }

    @Transactional
    public void changePassword(PasswordResetRequest request) {
        Optional<UserEntity> usuarioOptional = repository.findByEmail(request.getEmail());
        if(usuarioOptional.isEmpty()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s não foi encontrado.", request.getEmail()));
        }
        UserEntity user = usuarioOptional.get();

        PasswordResetToken token = passwordResetTokenPersistenceAdapter.findToken(user, request.getPasswordResetToken());

        if (token == null) {
            throw new ArchbaseValidationException("Token de redefinição de senha inválido.");
        }
        token.updateExpired();
        passwordResetTokenPersistenceAdapter.save(token);

        if (token.isExpired()) {
            throw new ArchbaseValidationException("Token de redefinição de senha expirado, favor gerar novamente.");
        }

        if (token.isRevoked()) {
            throw new ArchbaseValidationException("Token de redefinição de senha inválido, favor utilizar o token mais recente.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        repository.save(user);
        token.revokeToken();
        passwordResetTokenPersistenceAdapter.save(token);

        // Revogar tokens de acesso ao alterar a senha
        revokeAllUserTokens(user);
    }

}