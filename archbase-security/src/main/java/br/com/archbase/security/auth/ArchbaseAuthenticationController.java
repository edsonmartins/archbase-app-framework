package br.com.archbase.security.auth;

import br.com.archbase.security.service.ArchbaseAuthenticationService;
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
    
    @Autowired(required = false)
    private AuthenticationBusinessDelegate businessDelegate;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
        @RequestBody AuthenticationRequest request
    ) {
        try {
            return ResponseEntity.ok(service.authenticate(request));
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
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno do servidor");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshToken) {
        try {
            return ResponseEntity.ok(service.refreshToken(refreshToken));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            service.resetPassword(request);
            return ResponseEntity.ok().build();
        } catch (ArchbaseValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
                .body(Map.of("error", e.getMessage()));
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
            
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciais inválidas"));
        } catch (Exception e) {
            log.error("Erro no login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
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
            
        } catch (Exception e) {
            log.error("Erro no login social: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
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
    
    private Map<String, Object> validateSocialToken(String provider, String token) {
        // Implementação básica - em produção usar SDK do provedor
        // Por enquanto apenas validação simples
        if (token == null || token.isEmpty()) {
            throw new ArchbaseValidationException("Token inválido");
        }
        
        // Simular dados do provedor
        Map<String, Object> data = new HashMap<>();
        data.put("provider", provider);
        data.put("token", token);
        
        // Em produção, estes dados viriam do provedor
        // data.put("email", decodedToken.getEmail());
        // data.put("name", decodedToken.getName());
        // data.put("picture", decodedToken.getPicture());
        
        return data;
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
