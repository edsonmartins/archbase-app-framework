package br.com.archbase.security.service;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.adapter.AccessTokenPersistenceAdapter;
import br.com.archbase.security.adapter.PasswordResetTokenPersistenceAdapter;
import br.com.archbase.security.auth.*;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.*;
import br.com.archbase.security.exception.ArchbaseTooManyAttemptsException;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.password.ArchbasePasswordStrengthPolicy;
import br.com.archbase.security.ratelimit.ArchbaseAuthRateLimiter;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.UserGroupEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.token.TokenType;
import br.com.archbase.security.token.TokenUse;
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
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
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
    private final ArchbaseAuthRateLimiter rateLimiter;
    private final ArchbasePasswordStrengthPolicy passwordStrengthPolicy;

    // Injection opcional de enrichers - não quebra se não existir nenhum
    @Autowired(required = false)
    private List<AuthenticationResponseEnricher> enrichers;

    /**
     * Delegate de regra de negócio da aplicação. <b>Opcional</b> — todo uso abaixo é guardado por
     * {@code businessDelegate != null}.
     *
     * <p>Era {@code @Autowired} obrigatório, e o único candidato,
     * {@link DefaultAuthenticationBusinessDelegate}, é um {@code @Component} anotado com
     * {@code @ConditionalOnMissingBean} — condição que só é confiável em classe de
     * autoconfiguração, não em componente varrido: a avaliação depende da ordem do scan. Quando
     * ela decidia não registrar, o contexto inteiro falhava na subida por dependência não
     * satisfeita, num serviço que já estava escrito para funcionar sem o delegate.
     */
    @Autowired(required = false)
    private AuthenticationBusinessDelegate businessDelegate;

    // MFA/2FA - opcional; ausente ou desabilitado para o usuário ⇒ fluxo de login inalterado.
    @Autowired(required = false)
    private br.com.archbase.security.mfa.MfaService mfaService;

    /**
     * Rótulo de apresentação dos tenants — <b>opcional</b>. Sem bean registrado, o framework devolve
     * apenas o {@code tenantId}, porque não tem cadastro de organização de onde tirar um nome.
     * Ver {@link ArchbaseTenantInfoResolver}.
     */
    @Autowired(required = false)
    private ArchbaseTenantInfoResolver tenantInfoResolver;

    /**
     * Faz o pedido de reset responder igual para e-mail cadastrado e não cadastrado.
     *
     * <p>Desligado por padrão porque muda o contrato do endpoint: hoje ele responde 400 com
     * "usuário não encontrado", e telas que exibem essa mensagem deixariam de recebê-la. Ligue
     * junto com o ajuste no frontend para "se o e-mail estiver cadastrado, você receberá as
     * instruções".
     */
    @org.springframework.beans.factory.annotation.Value("${archbase.security.prevent-user-enumeration:false}")
    private boolean preventUserEnumeration;

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
        String rateLimitKey = ArchbaseAuthRateLimiter.key("login", request.getEmail());
        if (rateLimiter.isBlocked(rateLimitKey)) {
            log.warn("Login bloqueado por excesso de tentativas: {}", request.getEmail());
            throw new ArchbaseTooManyAttemptsException(
                    "Muitas tentativas de login. Tente novamente em alguns minutos.",
                    rateLimiter.secondsUntilUnblock(rateLimitKey));
        }
        try {
            // Resolver o tenant ANTES da autenticação, de modo que o
            // authenticationManager.authenticate e o findByEmail subsequente
            // resolvam DENTRO do tenant correto (o @Filter de tenant é aplicado).
            String tenant = request.getTenantId();
            if (tenant == null || tenant.isBlank()) {
                // Query nativa: ignora o @Filter, então enxerga todos os tenants.
                var opts = repository.findTenantsByEmailIgnoringTenant(request.getEmail());
                if (opts.size() == 1) {
                    tenant = (String) opts.get(0)[0];
                } else if (opts.size() > 1) {
                    // Múltiplos tenants: o frontend deve exibir o seletor de tenant.
                    throw new ArchbaseValidationException("Selecione o tenant para efetuar login");
                }
                // Se vazio: mantém tenant nulo (caminho existente de "usuário não encontrado").
            }

            if (tenant != null && !tenant.isBlank()) {
                ArchbaseTenantContext.setTenantId(tenant);
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Senha conferiu: zera a contagem para o usuário legítimo não carregar o histórico de
            // tentativas de um atacante que usou o mesmo e-mail.
            rateLimiter.recordSuccess(rateLimitKey);

            var user = repository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado"));

            // Segundo fator (MFA): senha conferiu, mas falta o TOTP. Não emite tokens ainda —
            // devolve um desafio; o cliente completa em POST /auth/mfa/verify.
            if (mfaService != null && mfaService.isMfaEnabled(user)) {
                log.debug("MFA requerido para usuário {}, emitindo desafio", user.getEmail());
                return AuthenticationResponse.builder()
                        .mfaRequired(true)
                        .challengeToken(jwtService.generateMfaChallengeToken(user).token())
                        .build();
            }

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
                // Token ainda válido, retorna o mesmo — mas o refresh é reemitido, e os anteriores
                // precisam morrer junto. Sem isto, cada login dentro da validade do access token
                // deixava mais um refresh vivo (dez logins, dez refresh válidos simultâneos),
                // desfazendo na prática a rotação e a revogação que este fluxo existe para garantir.
                //
                // Só que isso vale para quem NÃO pode ter várias sessões. Revogar sempre ignorava
                // allowMultipleLogins — o campo com que o framework modela exatamente esta
                // permissão — e derrubava sessões legítimas: abrir uma segunda aba, recarregar a
                // página ou qualquer caminho que reautentique matava o refresh que a primeira
                // sessão guardava. Ela seguia viva enquanto o access token durasse e, na primeira
                // renovação, era negada e deslogava o usuário sem que ele tivesse feito nada.
                if (!Boolean.TRUE.equals(user.getAllowMultipleLogins())) {
                    revokeAllRefreshTokens(user);
                }
                return buildAuthenticationResponse(accessToken, issueRefreshToken(user), user);
            }

            log.debug("Nenhum token válido encontrado para o usuário {}, criando novo token", user.getEmail());
            // Sempre revogar tokens antigos antes de criar novos
            revokeAllUserTokens(user);

            // Gerar novos tokens
            var jwtToken = jwtService.generateToken(user);
            accessToken = saveUserToken(user, jwtToken);

            return buildAuthenticationResponse(accessToken, issueRefreshToken(user), user);
        } catch (CredentialsExpiredException e) {
            log.warn("Credenciais expiradas para usuário: {}", request.getEmail());
            throw e; // Re-lançar para tratamento específico no controller
        } catch (AuthenticationException e) {
            // Só senha errada conta como tentativa. Credencial expirada não entra aqui de propósito:
            // é uma falha do estado da conta, não um palpite, e trancaria quem já está travado.
            rateLimiter.recordFailure(rateLimitKey);
            log.warn("Falha na autenticação", e);
            throw new BadCredentialsException("Login ou senha inválido", e);
        }
        // Sem finally { clear() }. Ele existia porque, com a limpeza do interceptor no postHandle,
        // um login que falhasse deixava o tenant na thread do pool. O interceptor agora limpa no
        // afterCompletion, que roda mesmo com exceção — e limpar aqui atrapalhava o caminho de
        // sucesso: o /login-flexible chama os hooks da aplicação (postAuthenticate, enrichers)
        // DEPOIS deste método, e eles abriam sessão nova sem tenant no contexto, resolvendo para o
        // tenant padrão. Ou seja: leitura (e possível gravação) no tenant errado, em silêncio.
    }

    /**
     * Completa o login em duas etapas (MFA): valida o token de desafio (emitido pelo
     * {@link #authenticate}) e o código do segundo fator (TOTP ou código de recuperação);
     * se ambos conferem, emite os tokens reais. O desafio carrega o tenant como claim.
     *
     * @throws BadCredentialsException se o desafio for inválido/expirado ou o código não conferir
     */
    @Transactional
    public AuthenticationResponse completeMfaAuthentication(String challengeToken, String code) {
        try {
            if (challengeToken == null || !jwtService.isMfaChallengeToken(challengeToken)) {
                throw new BadCredentialsException("Desafio de MFA inválido ou expirado");
            }
            String tenant = jwtService.extractTenantId(challengeToken);
            if (tenant != null && !tenant.isBlank()) {
                ArchbaseTenantContext.setTenantId(tenant);
            }
            String email = jwtService.extractUsername(challengeToken);

            // Segundo fator é um código de 6 dígitos: sem contagem de tentativas, o desafio de 5
            // minutos é tempo de sobra para varrer boa parte do espaço.
            String rateLimitKey = ArchbaseAuthRateLimiter.key("mfa", email);
            if (rateLimiter.isBlocked(rateLimitKey)) {
                log.warn("Verificação de MFA bloqueada por excesso de tentativas: {}", email);
                throw new ArchbaseTooManyAttemptsException(
                        "Muitas tentativas de verificação. Tente novamente em alguns minutos.",
                        rateLimiter.secondsUntilUnblock(rateLimitKey));
            }

            var user = repository.findByEmail(email)
                    .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado"));

            if (mfaService == null || !mfaService.verificar(user, code)) {
                rateLimiter.recordFailure(rateLimitKey);
                throw new BadCredentialsException("Código de verificação inválido");
            }
            rateLimiter.recordSuccess(rateLimitKey);

            // Segundo fator confirmado — emite tokens novos (revoga os antigos).
            accessTokenPersistenceAdapter.markExpiredTokens();
            revokeAllUserTokens(user);
            var jwtToken = jwtService.generateToken(user);
            var accessToken = saveUserToken(user, jwtToken);
            return buildAuthenticationResponse(accessToken, issueRefreshToken(user), user);
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    private AccessTokenEntity saveUserToken(UserEntity usuario, ArchbaseJwtService.TokenResult jwtToken) {
        return saveUserToken(usuario, jwtToken, TokenUse.ACCESS);
    }

    private AccessTokenEntity saveUserToken(UserEntity usuario, ArchbaseJwtService.TokenResult jwtToken, TokenUse tokenUse) {
        // Usar UTC para datas de expiração
        LocalDateTime expirationDateTime = convertToLocalDateTimeViaInstant(jwtService.extractExpiration(jwtToken.token()));

        log.debug("Salvando novo token ({}) para usuário {} com expiração em {}",
                tokenUse, usuario.getEmail(), expirationDateTime);

        var token = AccessTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .user(usuario)
                .token(jwtToken.token())
                .expirationTime(jwtToken.expiresIn())
                .expirationDate(expirationDateTime)
                .tokenType(TokenType.BEARER)
                .tokenUse(tokenUse)
                .expired(false)
                .revoked(false)
                .build();
        return tokenRepository.save(token);
    }

    /**
     * Emite e persiste o refresh token.
     *
     * <p>Persistir é o ponto: enquanto o refresh existia só como JWT assinado, nada no sistema
     * conseguia invalidá-lo — logout, reset de senha e revogação de sessão mexiam apenas nas linhas
     * de access token, e o refresh vazado seguia produzindo credenciais novas até a expiração
     * natural. Com a linha em banco, {@link #revokeAllUserTokens} alcança os dois.
     */
    private String issueRefreshToken(UserEntity user) {
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(user, refreshToken, TokenUse.REFRESH);
        return refreshToken.token();
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

    /**
     * Revoga apenas os refresh tokens do usuário, preservando o access token em uso.
     *
     * <p>Serve ao login que reaproveita um access token ainda válido: o refresh é reemitido, e
     * deixar os anteriores vivos acumularia credenciais de renovação sem limite.
     */
    @Transactional
    public void revokeAllRefreshTokens(UserEntity user) {
        var refreshTokens = accessTokenPersistenceAdapter.findAllValidTokenByUser(user).stream()
                .filter(token -> token.getTokenUse() == TokenUse.REFRESH)
                .toList();
        if (refreshTokens.isEmpty()) {
            return;
        }
        log.debug("Revogando {} refresh token(s) anteriores do usuário {}", refreshTokens.size(), user.getEmail());
        refreshTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(refreshTokens);
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
            // Só um token emitido COMO refresh entra aqui. Sem esta checagem, qualquer JWT assinado
            // com o subject do usuário servia — inclusive o desafio de MFA, que é emitido depois da
            // senha conferir e antes do segundo fator: trocá-lo aqui devolvia os tokens reais e o
            // segundo fator deixava de existir.
            if (!jwtService.isRefreshToken(refreshToken.getToken())) {
                log.warn("Refresh negado: token apresentado não é um refresh token");
                throw new JwtException("Token de refresh inválido");
            }

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

            // Assinatura válida não basta: o token precisa corresponder a uma linha viva. É o que
            // faz logout, troca de senha e revogação de sessão realmente encerrarem a renovação —
            // um refresh revogado continua com assinatura boa até a data de expiração.
            //
            // A tolerância é estreita de propósito: só um refresh SEM o claim token_use é anterior
            // a esta versão e, portanto, legitimamente não tem linha em banco. Tendo o claim, foi
            // emitido por este código e a linha existe — a ausência dela significa revogado, e aí
            // não há o que tolerar. Assim a revogação vale de imediato para todo token novo, e a
            // atualização não derruba as sessões que já estavam em curso.
            boolean issuedByCurrentVersion = jwtService.extractTokenUse(refreshToken.getToken()) != null;
            AccessTokenEntity storedRefreshToken =
                    accessTokenPersistenceAdapter.findRefreshTokenByValue(refreshToken.getToken());
            if (storedRefreshToken == null && issuedByCurrentVersion) {
                log.warn("Refresh negado: token revogado, expirado ou desconhecido para o usuário {}", userEmail);
                throw new JwtException("Token de refresh inválido");
            }

            // O estado da conta é reavaliado a cada refresh: sem isto, uma conta desativada,
            // bloqueada ou marcada para troca obrigatória de senha continuaria renovando tokens
            // indefinidamente, driblando as checagens feitas no login.
            if (!user.isEnabled()) {
                log.warn("Refresh negado: conta desativada ou bloqueada para o usuário {}", userEmail);
                throw new DisabledException("Conta desativada ou bloqueada");
            }
            if (!user.isCredentialsNonExpired()) {
                log.warn("Refresh negado: credenciais expiradas para o usuário {}", userEmail);
                throw new CredentialsExpiredException("As credenciais do usuário expiraram");
            }

            // Sempre revogar tokens antigos para evitar acumulação. Rotaciona o refresh junto:
            // o token apresentado deixa de valer assim que o novo par é emitido.
            revokeAllUserTokens(user);

            // Gerar novos tokens
            var jwtToken = jwtService.generateToken(user);
            AccessTokenEntity accessToken = saveUserToken(user, jwtToken);

            log.debug("Token refreshed com sucesso para o usuário: {}", userEmail);
            return buildAuthenticationResponse(accessToken, issueRefreshToken(user), user);

        } catch (JwtException e) {
            log.error("Erro ao processar refresh token", e);
            throw new JwtException("Erro ao processar token: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para construir resposta de autenticação.
     *
     * <p>Funil único de todos os retornos de login bem-sucedido — {@code /authenticate},
     * {@code /login}, {@code /login-flexible}, {@code /login-social} e refresh — por isso o tenant é
     * preenchido aqui uma vez só. A resposta de desafio MFA é montada à parte e de propósito não
     * traz tenant: ali o login ainda não se completou.
     */
    private AuthenticationResponse buildAuthenticationResponse(AccessTokenEntity accessToken, String refreshToken, UserEntity user) {
        return AuthenticationResponse.builder()
                .id(accessToken.getId())
                .accessToken(accessToken.getToken())
                .expirationTime(accessToken.getExpirationTime())
                .tokenType(TokenType.BEARER)
                .refreshToken(refreshToken)
                .user(user != null ? user.toDomain() : null)
                .tenant(user != null ? describeTenant(user.getTenantId()) : null)
                .build();
    }


    public void sendResetPasswordEmail(String email)  {
        Optional<UserEntity> usuarioOptional = repository.findByEmail(email);
        if(usuarioOptional.isEmpty()) {
            if (preventUserEnumeration) {
                // Responder "não encontrado" transforma o endpoint anônimo de reset numa consulta
                // de quem tem conta aqui — útil para montar lista de alvos antes de tentar senha.
                // Com a proteção ligada, e-mail existente e inexistente produzem a mesma resposta.
                log.info("Solicitação de reset para e-mail não cadastrado (resposta uniforme)");
                return;
            }
            throw new ArchbaseValidationException(String.format("Usuário com email %s  não foi encontrado.",email));
        }
        UserEntity user = usuarioOptional.get();
        revokeExistingTokens(user);
        // Coluna nula (base legada) é tratada como "pode alterar": o padrão do cadastro é true e
        // negar o reset por ausência de dado trancaria o usuário fora da conta.
        if (!Boolean.FALSE.equals(user.getAllowPasswordChange())) {
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

        // O token de reset tem 8 dígitos numéricos. Contar as tentativas é o que impede varrer o
        // espaço: sem isso, adivinhá-lo é só uma questão de quantas requisições cabem na validade.
        String rateLimitKey = ArchbaseAuthRateLimiter.key("reset", request.getEmail());
        if (rateLimiter.isBlocked(rateLimitKey)) {
            log.warn("Redefinição de senha bloqueada por excesso de tentativas: {}", request.getEmail());
            throw new ArchbaseTooManyAttemptsException(
                    "Muitas tentativas. Tente novamente em alguns minutos.",
                    rateLimiter.secondsUntilUnblock(rateLimitKey));
        }

        PasswordResetToken token = passwordResetTokenPersistenceAdapter.findToken(user, request.getPasswordResetToken());

        if (token == null) {
            rateLimiter.recordFailure(rateLimitKey);
            throw new ArchbaseValidationException("Token de redefinição de senha inválido.");
        }
        rateLimiter.recordSuccess(rateLimitKey);
        token.updateExpired();
        passwordResetTokenPersistenceAdapter.save(token);

        if (token.isExpired()) {
            throw new ArchbaseValidationException("Token de redefinição de senha expirado, favor gerar novamente.");
        }

        if (token.isRevoked()) {
            throw new ArchbaseValidationException("Token de redefinição de senha inválido, favor utilizar o token mais recente.");
        }

        passwordStrengthPolicy.validate(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // A troca obrigatória foi cumprida com token válido: limpa a exigência e
        // reinicia a contagem da expiração periódica.
        user.markPasswordChanged();

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

        // O token de reset tem 8 dígitos numéricos. Contar as tentativas é o que impede varrer o
        // espaço: sem isso, adivinhá-lo é só uma questão de quantas requisições cabem na validade.
        String rateLimitKey = ArchbaseAuthRateLimiter.key("reset", request.getEmail());
        if (rateLimiter.isBlocked(rateLimitKey)) {
            log.warn("Redefinição de senha bloqueada por excesso de tentativas: {}", request.getEmail());
            throw new ArchbaseTooManyAttemptsException(
                    "Muitas tentativas. Tente novamente em alguns minutos.",
                    rateLimiter.secondsUntilUnblock(rateLimitKey));
        }

        PasswordResetToken token = passwordResetTokenPersistenceAdapter.findToken(user, request.getPasswordResetToken());

        if (token == null) {
            rateLimiter.recordFailure(rateLimitKey);
            throw new ArchbaseValidationException("Token de redefinição de senha inválido.");
        }
        rateLimiter.recordSuccess(rateLimitKey);
        token.updateExpired();
        passwordResetTokenPersistenceAdapter.save(token);

        if (token.isExpired()) {
            throw new ArchbaseValidationException("Token de redefinição de senha expirado, favor gerar novamente.");
        }

        if (token.isRevoked()) {
            throw new ArchbaseValidationException("Token de redefinição de senha inválido, favor utilizar o token mais recente.");
        }

        passwordStrengthPolicy.validate(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.markPasswordChanged();

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

            // 2b. MFA pendente: devolve o desafio direto, sem pós-processar/enriquecer
            // (baseResponse ainda não tem usuário nem tokens).
            if (Boolean.TRUE.equals(baseResponse.getMfaRequired())) {
                return baseResponse;
            }

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

        } catch (CredentialsExpiredException e) {
            log.warn("Credenciais expiradas para usuário: {}", contextualRequest.getEmail());
            throw e; // Re-lançar para tratamento específico no controller
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

        log.debug("Autenticação sem senha bem-sucedida para: {}", email);

        return buildAuthenticationResponse(accessToken, issueRefreshToken(user), user);
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
     * Lista os tenants disponíveis para um email (pré-login).
     * Utiliza query nativa que ignora o @Filter de tenant, enxergando todos os tenants.
     * Se o email não possuir usuários, retorna lista vazia.
     *
     * <p><b>Só o {@code tenantId} sai daqui por conta própria.</b> As colunas {@code NOME} e
     * {@code DESCRICAO} desta consulta são as da linha de <b>usuário</b> — o nome e a descrição da
     * pessoa, não da organização. Devolvê-las expunha o nome do titular de qualquer e-mail
     * conhecido, num endpoint anônimo, e ainda fazia o seletor de tenant exibir o nome do próprio
     * usuário no lugar da empresa. O rótulo agora vem do {@link ArchbaseTenantInfoResolver}, que a
     * aplicação registra se tiver cadastro de organizações; sem ele, o cliente recebe o id.
     *
     * @param email Email a consultar
     * @return Lista de tenants disponíveis para login com esse email
     */
    public List<TenantLoginOption> findTenantsByEmail(String email) {
        return repository.findTenantsByEmailIgnoringTenant(email).stream()
                .map(opt -> opt[0] != null ? opt[0].toString() : null)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::describeTenant)
                .collect(Collectors.toList());
    }

    /**
     * Monta o descritor de um tenant: o id sempre, o rótulo só quando a aplicação souber informá-lo.
     *
     * <p>Falha do resolver não derruba o login nem a listagem — o id sozinho é suficiente para o
     * cliente funcionar, e um cadastro de organizações indisponível não é motivo para negar acesso.
     */
    private TenantLoginOption describeTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        if (tenantInfoResolver != null) {
            try {
                TenantLoginOption resolvido = tenantInfoResolver.resolve(tenantId);
                if (resolvido != null) {
                    resolvido.setTenantId(tenantId);
                    return resolvido;
                }
            } catch (Exception e) {
                log.warn("Resolver de tenant falhou para {}; devolvendo apenas o id", tenantId, e);
            }
        }
        return TenantLoginOption.builder().tenantId(tenantId).build();
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