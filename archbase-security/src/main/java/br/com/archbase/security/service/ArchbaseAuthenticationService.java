package br.com.archbase.security.service;

import br.com.archbase.security.adapter.AccessTokenPersistenceAdapter;
import br.com.archbase.security.adapter.PasswordResetTokenPersistenceAdapter;
import br.com.archbase.security.auth.*;
import br.com.archbase.security.domain.entity.PasswordResetToken;
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
public class ArchbaseAuthenticationService {
    private final UserJpaRepository repository;
    private final AccessTokenJpaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArchbaseJwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ArchbaseEmailService archbaseEmailService;
    private final PasswordResetTokenPersistenceAdapter passwordResetTokenPersistenceAdapter;
    private final AccessTokenPersistenceAdapter accessTokenPersistenceAdapter;

    public AuthenticationResponse register(RegisterNewUser request) {
        Optional<UserEntity> byEmail = repository.findByEmail(request.getEmail());
        UserEntity user = null;
        UserEntity savedUser = null;
        if (byEmail.isEmpty()) {
            user = UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createEntityDate(LocalDateTime.now())
                    .name(request.getName())
                    .description(request.getDescription())
                    .email(request.getEmail())
                    .userName(request.getUserName())
                    .passwordNeverExpires(true)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .build();
            user = repository.save(user);
        } else {
            user = byEmail.get();
        }
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        AccessTokenEntity savedUserToken = saveUserToken(user, jwtToken);
        return AuthenticationResponse.builder()
                .id(savedUserToken.getId())
                .accessToken(jwtToken.token()).expirationTime(jwtToken.expiresIn()).tokenType(TokenType.BEARER)
                .refreshToken(refreshToken.token())
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Login ou senha inválido", e);
        }
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();

        AccessTokenEntity accessToken = accessTokenPersistenceAdapter.findValidTokenByUser(user);

        if (accessToken == null || jwtService.isTokenExpired(accessToken.getToken())) {
            var jwtToken = jwtService.generateToken(user);
            revokeAllUserTokens(user);
            accessToken = saveUserToken(user, jwtToken);
        }

        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .id(accessToken.getId())
                .accessToken(accessToken.getToken()).expirationTime(accessToken.getExpirationTime()).tokenType(TokenType.BEARER)
                .refreshToken(refreshToken.token())
                .build();
    }

    private AccessTokenEntity saveUserToken(UserEntity usuario, ArchbaseJwtService.TokenResult jwtToken) {
        var token = AccessTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .user(usuario)
                .token(jwtToken.token())
                .expirationTime(jwtToken.expiresIn())
                .expirationDate(convertToLocalDateTimeViaMilisecond(jwtService.extractExpiration(jwtToken.token())))
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        return tokenRepository.save(token);
    }

    public LocalDateTime convertToLocalDateTimeViaMilisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private void revokeAllUserTokens(UserEntity user) {
        var validUserTokens = accessTokenPersistenceAdapter.findAllValidTokenByUser(user);
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshToken) {
        String userEmail = jwtService.extractUsername(refreshToken.getToken());
        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken.getToken(), user)) {

                AccessTokenEntity accessToken = accessTokenPersistenceAdapter.findValidTokenByUser(user);

                if (accessToken == null || jwtService.isTokenExpired(accessToken.getToken())) {
                    var jwtToken = jwtService.generateToken(user);
                    revokeAllUserTokens(user);
                    accessToken = saveUserToken(user, jwtToken);
                }

                var newRefreshToken = jwtService.generateRefreshToken(user);

                return AuthenticationResponse.builder()
                        .id(accessToken.getId())
                        .accessToken(accessToken.getToken()).expirationTime(accessToken.getExpirationTime()).tokenType(TokenType.BEARER)
                        .refreshToken(newRefreshToken.token())
                        .build();
            }
        }
        throw new JwtException("Token inválido");
    }

    public void sendResetPasswordEmail(String email)  {
        Optional<UserEntity> usuarioOptional = repository.findByEmail(email);
        if(usuarioOptional.isEmpty()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s  não foi encontrado.",email));
        }
        UserEntity user = usuarioOptional.get();
        revokeExistingTokens(user);
        if (user.getAllowPasswordChange()) {
            String passwordResetToken = createPasswordResetToken(user);
            archbaseEmailService.sendResetPasswordEmail(email, passwordResetToken, user.getUsername(), user.getName());
        } else {
            throw new ArchbaseValidationException(String.format("Usuário com email %s  não possui autorização para alterar a senha.",email));
        }
    }

    private String createPasswordResetToken(UserEntity user) {
        String passwordResetToken = TokenGeneratorUtil.generateAlphaNumericToken();
        PasswordResetToken token = new PasswordResetToken(passwordResetToken, user.toDomain());
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
    }
}
