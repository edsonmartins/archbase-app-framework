package br.com.archbase.security.auth;

import br.com.archbase.security.exception.ArchbaseTooManyAttemptsException;
import br.com.archbase.security.ratelimit.ArchbaseAuthRateLimiter;
import br.com.archbase.security.service.ArchbaseAuthenticationService;
import br.com.archbase.security.spi.ArchbaseSocialTokenValidator;
import br.com.archbase.security.service.ArchbaseUserService;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints de autenticação e autorização")
public class ArchbaseAuthenticationController {

    private final ArchbaseAuthenticationService service;
    private final ArchbaseUserService userService;
    private final ArchbaseAuthRateLimiter rateLimiter;

    @Autowired(required = false)
    private AuthenticationBusinessDelegate businessDelegate;

    /** Validadores de login social registrados pela aplicação; vazio = login social indisponível. */
    @Autowired(required = false)
    private List<ArchbaseSocialTokenValidator> socialTokenValidators = List.of();

    /**
     * 429 com {@code Retry-After}: o cliente precisa distinguir "credencial errada" de "pare de
     * tentar por enquanto". Devolver 401 de novo faria um app com retry automático continuar
     * batendo e prolongar o próprio bloqueio.
     */
    /**
     * Mensagem que pode ir para o cliente.
     *
     * <p>Só a de {@link ArchbaseValidationException}, que é escrita para o usuário final. Qualquer
     * outra exceção carrega detalhe interno — nome de tabela, coluna e fragmento de SQL numa
     * violação de integridade, por exemplo — e estes handlers atendem endpoints anônimos como
     * {@code /auth/register}. O diagnóstico completo fica no log.
     *
     * <p>Nunca devolve {@code null}: {@code Map.of} rejeita valor nulo, e uma exceção sem mensagem
     * (um {@code NullPointerException} vindo de um delegate, por exemplo) faria o próprio bloco de
     * tratamento estourar, trocando a resposta por um 500 sem corpo.
     */
    private String safeMessage(Exception e) {
        if (e instanceof ArchbaseValidationException && e.getMessage() != null) {
            return e.getMessage();
        }
        return "Não foi possível completar a operação.";
    }

    private ResponseEntity<?> tooManyAttempts(ArchbaseTooManyAttemptsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
            .body(Map.of(
                "error", "TOO_MANY_ATTEMPTS",
                "message", e.getMessage(),
                "retryAfterSeconds", e.getRetryAfterSeconds()
            ));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
        @RequestBody AuthenticationRequest request
    ) {
        try {
            return ResponseEntity.ok(service.authenticate(request));
        } catch (ArchbaseTooManyAttemptsException e) {
            return tooManyAttempts(e);
        } catch (CredentialsExpiredException e) {
            log.warn("Credenciais expiradas para usuário: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "CREDENTIALS_EXPIRED",
                    "message", "As credenciais do usuário expiraram. É necessário alterar a senha.",
                    "requirePasswordChange", true,
                    "email", request.getEmail()
                ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Passo 2 do login com MFA: recebe o token de desafio (emitido quando a senha conferiu e o
     * usuário tem MFA) + o código do segundo fator, e devolve os tokens reais se ambos conferem.
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request) {
        try {
            return ResponseEntity.ok(service.completeMfaAuthentication(request.challengeToken(), request.code()));
        } catch (ArchbaseTooManyAttemptsException e) {
            return tooManyAttempts(e);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Endpoint de autenticação contextual com suporte a enrichers.
     * Permite que aplicações personalizem a resposta de autenticação
     * baseada no contexto (STORE_APP, CUSTOMER_APP, etc.).
     * 
     * @param contextualRequest Request com contexto da aplicação
     * @param httpRequest Request HTTP para contexto adicional
     * @return Resposta de autenticação (possivelmente enriquecida)
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginWithContext(
            @RequestBody ContextualAuthenticationRequest contextualRequest,
            HttpServletRequest httpRequest) {
        try {
            AuthenticationResponse response = service.authenticateWithContext(
                contextualRequest,
                httpRequest
            );
            return ResponseEntity.ok(response);
        } catch (ArchbaseTooManyAttemptsException e) {
            return tooManyAttempts(e);
        } catch (CredentialsExpiredException e) {
            log.warn("Credenciais expiradas para usuário: {}", contextualRequest.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "CREDENTIALS_EXPIRED",
                    "message", "As credenciais do usuário expiraram. É necessário alterar a senha.",
                    "requirePasswordChange", true,
                    "email", contextualRequest.getEmail()
                ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro interno no login para usuario {}: {}", contextualRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Erro interno do servidor",
                    "detail", "Erro interno do servidor"
                ));
        }
    }

    /**
     * Lista os tenants disponíveis para um email (pré-login).
     * Quando um mesmo email pertence a múltiplos tenants, o frontend usa este
     * endpoint para exibir o seletor de tenant antes de chamar /authenticate.
     * Endpoint pré-autenticação (coberto pelo whitelist /api/v1/auth/**).
     *
     * <p><b>É um endpoint anônimo que responde sobre a existência de um e-mail</b>, então tem
     * contagem própria. Duas chaves, e qualquer uma basta para recusar:
     *
     * <ul>
     *   <li><b>por origem</b> — é a que contém enumeração de verdade. Varrer uma lista de e-mails
     *       usa um e-mail diferente por tentativa, o que daria orçamento novo a cada palpite se a
     *       contagem fosse só por e-mail; por origem, a varredura toda divide o mesmo orçamento.</li>
     *   <li><b>por e-mail</b> — contém o martelo sobre um alvo específico vindo de várias origens.</li>
     * </ul>
     *
     * <p>O escopo é separado do {@code "login"} de propósito: abusar da descoberta não pode trancar
     * o login legítimo de quem tem aquele e-mail — seria negação de serviço contra a vítima.
     */
    @GetMapping("/tenants")
    @Operation(summary = "Listar tenants para um email",
               description = "Retorna os tenants disponíveis para login com o email informado")
    public ResponseEntity<?> tenantsForEmail(@RequestParam("email") String email,
                                             HttpServletRequest httpRequest) {
        String chaveOrigem = ArchbaseAuthRateLimiter.key("tenants-ip", httpRequest.getRemoteAddr());
        String chaveEmail = ArchbaseAuthRateLimiter.key("tenants", email);

        for (String chave : List.of(chaveOrigem, chaveEmail)) {
            if (rateLimiter.isBlocked(chave)) {
                log.warn("Consulta de tenants bloqueada por excesso de tentativas");
                return tooManyAttempts(new ArchbaseTooManyAttemptsException(
                        "Muitas consultas. Tente novamente em alguns minutos.",
                        rateLimiter.secondsUntilUnblock(chave)));
            }
        }

        // Não existe "sucesso" aqui que justifique zerar a contagem: toda consulta é uma tentativa,
        // e uma consulta bem-sucedida é justamente o que o atacante quer repetir.
        rateLimiter.recordFailure(chaveOrigem);
        rateLimiter.recordFailure(chaveEmail);

        return ResponseEntity.ok(service.findTenantsByEmail(email));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshToken) {
        try {
            return ResponseEntity.ok(service.refreshToken(refreshToken));
        } catch (CredentialsExpiredException e) {
            log.warn("Refresh negado, credenciais expiradas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "CREDENTIALS_EXPIRED",
                    "message", "As credenciais do usuário expiraram. É necessário alterar a senha.",
                    "requirePasswordChange", true
                ));
        } catch (DisabledException e) {
            log.warn("Refresh negado, conta desativada ou bloqueada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "ACCOUNT_DISABLED",
                    "message", "Conta desativada ou bloqueada."
                ));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao renovar token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                // Sem e.getMessage(): a mensagem de exceção carrega nome de tabela, coluna e
                // fragmento de SQL. O diagnóstico fica no log, que é do operador — não na resposta,
                // que é de quem chamou.
                .body(Map.of("error", "INTERNAL_ERROR", "detail", "Erro interno do servidor"));
        }
    }


    @PostMapping("/sendResetPasswordEmail/{email}")
    public ResponseEntity<?> sendResetPasswordEmail(@PathVariable String email) {
        try {
            service.sendResetPasswordEmail(email);
            return ResponseEntity.ok().build();
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao enviar email de reset de senha para {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                // Sem e.getMessage(): a mensagem de exceção carrega nome de tabela, coluna e
                // fragmento de SQL. O diagnóstico fica no log, que é do operador — não na resposta,
                // que é de quem chamou.
                .body(Map.of("error", "INTERNAL_ERROR", "detail", "Erro interno do servidor"));
        }
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            service.resetPassword(request);
            return ResponseEntity.ok().build();
        } catch (ArchbaseTooManyAttemptsException e) {
            return tooManyAttempts(e);
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao resetar senha: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                // Sem e.getMessage(): a mensagem de exceção carrega nome de tabela, coluna e
                // fragmento de SQL. O diagnóstico fica no log, que é do operador — não na resposta,
                // que é de quem chamou.
                .body(Map.of("error", "INTERNAL_ERROR", "detail", "Erro interno do servidor"));
        }
    }
    
    /**
     * Registro de novo usuário com suporte a lógica de negócio customizada.
     * Delega criação de dados específicos para o businessDelegate se disponível.
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", 
               description = "Cria novo usuário no sistema com possibilidade de dados adicionais de negócio")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Criar usuário no sistema de segurança
            RegisterNewUser newUser = RegisterNewUser.builder()
                .email(request.getEmail())
                .name(request.getName())
                .userName(request.getEmail())
                .password(request.getPassword())
                .description(request.getName())
                .avatar(request.getAvatar())
                .build();
                
            service.register(newUser);
            
            // Se houver delegate, criar dados de negócio
            String businessId = null;
            if (businessDelegate != null && request.getAdditionalData() != null) {
                businessId = businessDelegate.onUserRegistered(
                    userService.findByEmail(request.getEmail()),
                    request.getAdditionalData()
                );
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", request.getEmail());
            response.put("businessId", businessId);
            response.put("message", "Usuário registrado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao registrar usuário: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", safeMessage(e)));
        }
    }
    
    /**
     * Login com identificação flexível (email ou telefone).
     * Suporta contexto e enriquecimento via businessDelegate.
     */
    @PostMapping("/login-flexible")
    @Operation(summary = "Login flexível", 
               description = "Login com email ou telefone e suporte a contexto")
    public ResponseEntity<?> loginFlexible(@Valid @RequestBody LoginRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            String context = request.getContext() != null ? 
                request.getContext() : getDefaultContext();
            
            // Validar contexto se houver delegate
            if (businessDelegate != null && !businessDelegate.supportsContext(context)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Contexto não suportado: " + context));
            }
            
            // Pre-autenticação
            if (businessDelegate != null) {
                businessDelegate.preAuthenticate(request.getIdentifier(), context);
            }
            
            // Determinar email a partir do identificador
            String email = resolveEmail(request.getIdentifier());
            
            // Autenticar
            AuthenticationRequest authRequest = AuthenticationRequest.builder()
                .email(email)
                .password(request.getPassword())
                .build();
                
            AuthenticationResponse response = service.authenticate(authRequest);
            
            // Pós-autenticação
            if (businessDelegate != null) {
                businessDelegate.postAuthenticate(response.getUser(), context);
                
                // Enriquecer resposta
                response = businessDelegate.enrichAuthenticationResponse(
                    response, context, httpRequest);
            }
            
            return ResponseEntity.ok(response);

        } catch (ArchbaseTooManyAttemptsException e) {
            return tooManyAttempts(e);
        } catch (CredentialsExpiredException e) {
            log.warn("Credenciais expiradas para usuário: {}", request.getIdentifier());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "CREDENTIALS_EXPIRED",
                    "message", "As credenciais do usuário expiraram. É necessário alterar a senha.",
                    "requirePasswordChange", true,
                    "identifier", request.getIdentifier()
                ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciais inválidas"));
        } catch (Exception e) {
            log.error("Erro no login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", safeMessage(e)));
        }
    }

    /**
     * Login com provedor social (Google, Facebook, etc).
     */
    @PostMapping("/login-social")
    @Operation(summary = "Login social", 
               description = "Autenticação via provedores externos como Google")
    public ResponseEntity<?> loginSocial(@Valid @RequestBody SocialLoginRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            // Validar token com provedor
            Map<String, Object> providerData = validateSocialToken(
                request.getProvider(), request.getToken());
                
            String email = (String) providerData.get("email");
            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email não fornecido pelo provedor"));
            }
            
            // Verificar se usuário existe
            boolean userExists = userService.existsByEmail(email);
            
            if (!userExists) {
                // Criar novo usuário
                RegisterNewUser newUser = RegisterNewUser.builder()
                    .email(email)
                    .name((String) providerData.get("name"))
                    .userName(email)
                    .password(generateSecurePassword()) // Senha aleatória segura
                    .avatar((byte[]) providerData.get("picture"))
                    .build();
                    
                service.register(newUser);
                
                // Criar dados de negócio se houver delegate
                if (businessDelegate != null) {
                    providerData.put("newUser", true);
                    businessDelegate.onSocialLogin(request.getProvider(), providerData);
                }
            }
            
            // Gerar tokens
            AuthenticationResponse response = service.authenticateWithoutPassword(email);
            
            // Enriquecer resposta
            String context = request.getContext() != null ? 
                request.getContext() : getDefaultContext();
                
            if (businessDelegate != null) {
                response = businessDelegate.enrichAuthenticationResponse(
                    response, context, httpRequest);
            }
            
            return ResponseEntity.ok(response);

        } catch (SocialLoginNotConfiguredException e) {
            log.warn("Login social solicitado sem validador registrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "SOCIAL_LOGIN_NOT_CONFIGURED", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro no login social: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", safeMessage(e)));
        }
    }
    
    /**
     * Lista contextos suportados pela aplicação.
     */
    @GetMapping("/contexts")
    @Operation(summary = "Listar contextos", 
               description = "Retorna todos os contextos de autenticação suportados")
    public ResponseEntity<ContextsResponse> getSupportedContexts() {
        List<String> contexts = businessDelegate != null ? 
            businessDelegate.getSupportedContexts() : List.of("DEFAULT");
            
        String defaultContext = businessDelegate != null ? 
            businessDelegate.getDefaultContext() : "DEFAULT";
            
        return ResponseEntity.ok(
            ContextsResponse.builder()
                .supportedContexts(contexts)
                .defaultContext(defaultContext)
                .build()
        );
    }
    
    /**
     * Valida se um contexto é suportado.
     */
    @GetMapping("/contexts/{context}/validate")
    @Operation(summary = "Validar contexto", 
               description = "Verifica se um contexto específico é suportado")
    public ResponseEntity<Map<String, Object>> validateContext(@PathVariable String context) {
        boolean supported = businessDelegate != null ? 
            businessDelegate.supportsContext(context) : "DEFAULT".equals(context);
            
        return ResponseEntity.ok(Map.of(
            "context", context,
            "supported", supported
        ));
    }
    
    // Métodos auxiliares
    
    private String getDefaultContext() {
        return businessDelegate != null ? 
            businessDelegate.getDefaultContext() : "DEFAULT";
    }
    
    private String resolveEmail(String identifier) {
        // Se já é email, retornar
        if (identifier.contains("@")) {
            return identifier;
        }
        
        // Se é telefone, buscar email associado
        if (identifier.matches("\\d{10,11}")) {
            // Delegar para businessDelegate se disponível
            // Por enquanto, lançar exceção
            throw new ArchbaseValidationException(
                "Login por telefone requer implementação específica");
        }
        
        throw new ArchbaseValidationException("Identificador inválido");
    }
    
    /**
     * Delega a validação ao {@link ArchbaseSocialTokenValidator} registrado pela aplicação.
     *
     * <p>A implementação anterior não validava nada: montava um mapa com o token recebido e
     * devolvia. O login só não acontecia porque o {@code email} vinha nulo — proteção por acidente.
     * Sem validador registrado, agora a recusa é explícita.
     */
    private Map<String, Object> validateSocialToken(String provider, String token) {
        if (token == null || token.isEmpty()) {
            throw new ArchbaseValidationException("Token inválido");
        }

        ArchbaseSocialTokenValidator validator = socialTokenValidators.stream()
                .filter(candidate -> candidate.supports(provider))
                .findFirst()
                .orElseThrow(() -> new SocialLoginNotConfiguredException(provider));

        Map<String, Object> data = validator.validate(provider, token);
        if (data == null || data.get("email") == null) {
            throw new ArchbaseValidationException("Provedor não retornou o e-mail do usuário");
        }
        return data;
    }

    /** Login social pedido sem validador registrado para o provedor. */
    static class SocialLoginNotConfiguredException extends RuntimeException {
        SocialLoginNotConfiguredException(String provider) {
            super("Login social não configurado para o provedor '" + provider
                    + "'. Registre um bean ArchbaseSocialTokenValidator.");
        }
    }
    
    private String generateSecurePassword() {
        // Gerar senha aleatória segura
        return java.util.UUID.randomUUID().toString();
    }
    
    // DTOs internos
    
    @lombok.Data
    @lombok.Builder
    public static class ContextsResponse {
        private List<String> supportedContexts;
        private String defaultContext;
    }
}
