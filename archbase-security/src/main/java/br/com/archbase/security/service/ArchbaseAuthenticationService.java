package br.com.archbase.security.service;

import br.com.archbase.security.adapter.AccessTokenPersistenceAdapter;
import br.com.archbase.security.adapter.PasswordResetTokenPersistenceAdapter;
import br.com.archbase.security.auth.*;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.*;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.UserGroupEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.token.TokenType;
import br.com.archbase.security.util.TokenGeneratorUtil;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchbaseAuthenticationService {
    private final UserJpaRepository repository;
    private final GroupService groupService;
    private final UserProfileService userProfileService;
    private final AccessTokenJpaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArchbaseJwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ArchbaseEmailService archbaseEmailService;
    private final UserService userService;
    private final PasswordResetTokenPersistenceAdapter passwordResetTokenPersistenceAdapter;
    private final AccessTokenPersistenceAdapter accessTokenPersistenceAdapter;

    // Injection opcional de enrichers - não quebra se não existir nenhum
    @Autowired(required = false)
    private List<AuthenticationResponseEnricher> enrichers;

    // Injection do business delegate - usa implementação padrão se não existir customizada
    @Autowired
    private AuthenticationBusinessDelegate businessDelegate;

    @Transactional
    public void register(RegisterNewUser request) {
        Optional<UserEntity> byEmail = repository.findByEmail(request.getEmail());
        List<Group> groups = groupService.findByNames(request.getGroupNames());
        Optional<Profile> profile = userProfileService.findByName(request.getProfileName());
        UserEntity user;
        if (byEmail.isEmpty()) {
            user = UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createEntityDate(LocalDateTime.now())
                    .name(request.getName())
                    .description(request.getDescription())
                    .email(request.getEmail())
                    .userName(request.getUserName())
                    .changePasswordOnNextLogin(request.getChangePasswordOnNextLogin())
                    .allowPasswordChange(request.getAllowPasswordChange() != null ? request.getAllowPasswordChange() : true)
                    .allowMultipleLogins(request.getAllowMultipleLogins())
                    .passwordNeverExpires(request.getPasswordNeverExpires() != null ? request.getPasswordNeverExpires() : true)
                    .accountDeactivated(request.getAccountDeactivated())
                    .accountLocked(request.getAccountLocked())
                    .unlimitedAccessHours(request.getUnlimitedAccessHours())
                    .isAdministrator(request.getIsAdministrator())
                    .avatar(request.getAvatar())
                    .password(request.getPassword())
                    .build();
            profile.ifPresent(value -> user.setProfile(ProfileEntity.fromDomain(value)));
            Set<UserGroupEntity> userGroups = groups.stream()
                    .map(group -> UserGroupEntity
                            .fromDomain(UserGroup.builder().group(group)
                            .build(), user))
                    .collect(Collectors.toSet());
            user.setGroups(userGroups);
            UserDto createdUser = userService.createUser(user.toDto());

            // Chamar delegate para lógica de negócio específica da aplicação
            if (businessDelegate != null) {
                try {
                    Map<String, Object> registrationData = createRegistrationDataMap(request);
                    String businessEntityId = businessDelegate.onUserRegistered(createdUser.toDomain(), registrationData);
                    log.debug("Business delegate criou entidade de negócio com ID: {} para usuário: {}",
                            businessEntityId, createdUser.getEmail());
                } catch (Exception e) {
                    log.error("Erro ao executar lógica de negócio após registro para usuário: {}",
                            createdUser.getEmail(), e);
                    // Não falha o registro por erro no delegate
                }
            }
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

            // Marcar tokens expirados antes de buscar tokens válidos
            int expiredCount = accessTokenPersistenceAdapter.markExpiredTokens();
            if (expiredCount > 0) {
                log.debug("Marcados {} tokens como expirados antes da autenticação", expiredCount);
            }

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

    /**
     * Autentica usuário com contexto específico da aplicação.
     * Aplica enrichers registrados para personalizar a resposta.
     *
     * @param contextualRequest Request com contexto da aplicação
     * @param httpRequest Request HTTP para contexto adicional
     * @return Resposta de autenticação enriquecida
     */
    @Transactional
    public AuthenticationResponse authenticateWithContext(
            ContextualAuthenticationRequest contextualRequest,
            HttpServletRequest httpRequest) {

        try {
            log.debug("Iniciando autenticação contextual para usuário: {} com contexto: {}",
                    contextualRequest.getEmail(), contextualRequest.getContext());

            // 1. Pré-autenticação: validações customizadas via delegate
            if (businessDelegate != null && contextualRequest.getContext() != null) {
                try {
                    businessDelegate.preAuthenticate(contextualRequest.getEmail(), contextualRequest.getContext());
                } catch (Exception e) {
                    log.warn("Falha na pré-autenticação para usuário: {} no contexto: {}",
                            contextualRequest.getEmail(), contextualRequest.getContext(), e);
                    throw new BadCredentialsException("Acesso negado para este contexto", e);
                }
            }

            // 2. Autenticação básica usando lógica existente
            AuthenticationResponse baseResponse = authenticate(contextualRequest.toBasicRequest());

            // 3. Pós-autenticação: ações customizadas via delegate
            if (businessDelegate != null && contextualRequest.getContext() != null) {
                try {
                    businessDelegate.postAuthenticate(baseResponse.getUser(), contextualRequest.getContext());
                } catch (Exception e) {
                    log.error("Erro na pós-autenticação para usuário: {} no contexto: {}",
                            contextualRequest.getEmail(), contextualRequest.getContext(), e);
                    // Não falha o login por erro no pós-processamento
                }
            }

            // 4. Enriquecimento via business delegate
            AuthenticationResponse enrichedResponse = baseResponse;
            if (businessDelegate != null && contextualRequest.getContext() != null) {
                try {
                    enrichedResponse = businessDelegate.enrichAuthenticationResponse(
                            baseResponse, contextualRequest.getContext(), httpRequest);
                } catch (Exception e) {
                    log.error("Erro no enriquecimento via business delegate para usuário: {} no contexto: {}",
                            contextualRequest.getEmail(), contextualRequest.getContext(), e);
                    // Continua com resposta base em caso de erro
                }
            }

            // 5. Aplicar enrichers legados se existirem (mantém compatibilidade)
            if (enrichers != null && !enrichers.isEmpty() && contextualRequest.getContext() != null) {
                enrichedResponse = applyEnrichers(enrichedResponse, contextualRequest.getContext(), httpRequest);
            }

            log.debug("Autenticação contextual concluída para usuário: {}",
                    contextualRequest.getEmail());
            return enrichedResponse;

        } catch (AuthenticationException e) {
            log.warn("Falha na autenticação contextual para usuário: {}", contextualRequest.getEmail(), e);
            throw new BadCredentialsException("Login ou senha inválido", e);
        }
    }

    /**
     * Aplica enrichers registrados à resposta de autenticação.
     * Enrichers são executados em ordem de prioridade (getOrder()).
     *
     * @param baseResponse Resposta básica de autenticação
     * @param context Contexto da aplicação
     * @param request Request HTTP original
     * @return Resposta enriquecida
     */
    private AuthenticationResponse applyEnrichers(
            AuthenticationResponse baseResponse,
            String context,
            HttpServletRequest request) {

        log.debug("Aplicando {} enrichers para contexto: {}", enrichers.size(), context);

        // Filtrar enrichers que suportam o contexto e ordenar por prioridade
        List<AuthenticationResponseEnricher> applicableEnrichers = enrichers.stream()
            .filter(enricher -> enricher.supports(context))
            .sorted(Comparator.comparing(AuthenticationResponseEnricher::getOrder))
            .collect(Collectors.toList());

        if (applicableEnrichers.isEmpty()) {
            log.debug("Nenhum enricher aplicável encontrado para contexto: {}", context);
            return baseResponse;
        }

        log.debug("Aplicando {} enrichers aplicáveis para contexto: {}",
                applicableEnrichers.size(), context);

        // Aplicar enrichers em sequência
        AuthenticationResponse enrichedResponse = baseResponse;
        for (AuthenticationResponseEnricher enricher : applicableEnrichers) {
            try {
                log.trace("Aplicando enricher: {} para contexto: {}",
                        enricher.getClass().getSimpleName(), context);

                enrichedResponse = enricher.enrich(enrichedResponse, context, request);

                log.trace("Enricher {} aplicado com sucesso", enricher.getClass().getSimpleName());

            } catch (Exception e) {
                log.error("Erro ao aplicar enricher {} para contexto {}: {}",
                        enricher.getClass().getSimpleName(), context, e.getMessage(), e);

                // Continuar com outros enrichers em caso de erro
                // O comportamento pode ser configurado conforme necessário
            }
        }

        log.debug("Enriquecimento concluído para contexto: {}", context);
        return enrichedResponse;
    }

    /**
     * Autentica usuário sem senha - usado para login social ou outros casos especiais.
     * Gera tokens JWT baseado apenas no email do usuário.
     *
     * @param email Email do usuário
     * @return Resposta de autenticação com tokens
     */
    @Transactional
    public AuthenticationResponse authenticateWithoutPassword(String email) {
        var user = repository.findByEmail(email)
                .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado"));

        // Verificar se usuário está ativo
        if (Boolean.TRUE.equals(user.getAccountDeactivated()) || Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new BadCredentialsException("Conta de usuário inativa ou bloqueada");
        }

        // Revogar tokens antigos
        revokeAllUserTokens(user);

        // Gerar novos tokens
        var jwtToken = jwtService.generateToken(user);
        AccessTokenEntity accessToken = saveUserToken(user, jwtToken);
        var refreshToken = jwtService.generateRefreshToken(user);

        log.debug("Autenticação sem senha bem-sucedida para: {}", email);

        return buildAuthenticationResponse(accessToken, refreshToken.token(), user);
    }

    /**
     * Verifica se um usuário existe pelo email.
     *
     * @param email Email a verificar
     * @return true se usuário existe
     */
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    /**
     * Cria mapa com dados de registro para o business delegate.
     */
    private Map<String, Object> createRegistrationDataMap(RegisterNewUser request) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", request.getName());
        data.put("description", request.getDescription());
        data.put("email", request.getEmail());
        data.put("userName", request.getUserName());
        data.put("avatar", request.getAvatar());
        data.put("isAdministrator", request.getIsAdministrator());
        data.put("changePasswordOnNextLogin", request.getChangePasswordOnNextLogin());
        data.put("allowPasswordChange", request.getAllowPasswordChange());
        data.put("allowMultipleLogins", request.getAllowMultipleLogins());
        data.put("passwordNeverExpires", request.getPasswordNeverExpires());
        data.put("accountDeactivated", request.getAccountDeactivated());
        data.put("accountLocked", request.getAccountLocked());
        data.put("unlimitedAccessHours", request.getUnlimitedAccessHours());
        return data;
    }

}