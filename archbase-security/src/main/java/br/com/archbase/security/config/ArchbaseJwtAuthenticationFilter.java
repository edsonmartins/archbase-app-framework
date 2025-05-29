package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.adapter.AccessTokenPersistenceAdapter;
import br.com.archbase.security.domain.entity.ApiToken;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.service.ApiTokenService;
import br.com.archbase.security.service.ArchbaseJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.Enumeration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchbaseJwtAuthenticationFilter extends OncePerRequestFilter {

    private final ArchbaseJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AccessTokenJpaRepository tokenRepository;
    private final ApiTokenService apiTokenService;
    private final AccessTokenPersistenceAdapter accessTokenPersistenceAdapter;

    public static final String X_TENANT_ID = "X-TENANT-ID";
    public static final String X_COMPANY_ID = "X-COMPANY-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String tokenParam = request.getParameter("token");

        // Log de diagnóstico - URL e método
        log.info("Requisição recebida: {} {}", request.getMethod(), request.getRequestURI());

        // Log de diagnóstico - headers completos
        logAllHeaders(request);

        String tenantId = request.getHeader(X_TENANT_ID);
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = request.getParameter(X_TENANT_ID);
        }
        log.info("TenantID recebido: {}", tenantId);

        String companyId = request.getHeader(X_COMPANY_ID);
        if (companyId == null || companyId.isEmpty()) {
            companyId = request.getParameter(X_COMPANY_ID);
        }
        log.info("CompanyID recebido: {}", companyId);

        if (tenantId != null && !tenantId.isEmpty()) {
            ArchbaseTenantContext.setTenantId(tenantId);
            log.info("TenantID definido no contexto: {}", tenantId);
        } else {
            log.warn("TenantID não fornecido na requisição");
        }

        if (companyId != null && !companyId.isEmpty()) {
            ArchbaseTenantContext.setCompanyId(companyId);
            log.info("CompanyID definido no contexto: {}", companyId);
        }

        try {
            // Log de diagnóstico - token
            log.info("Authorization header: {}", authHeader);
            log.info("Token parameter: {}", tokenParam);

            // Processar header de autorização
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    // Token JWT (usuário/senha)
                    String token = authHeader.substring(7);
                    log.info("Processando Bearer token: {}", maskToken(token));
                    processJwtToken(token, request);
                } else if (isValidUUID(authHeader)) {
                    // Token API (UUID direto)
                    log.info("Processando API token (UUID): {}", maskUUID(authHeader));
                    processApiToken(authHeader, request);
                } else {
                    log.warn("Formato de autorização não reconhecido: {}", authHeader);
                }
            }
            // Se não tem header, tenta pegar token JWT da URL
            else if (tokenParam != null) {
                if (isValidUUID(tokenParam)) {
                    log.info("Processando API token da URL (UUID): {}", maskUUID(tokenParam));
                    processApiToken(tokenParam, request);
                } else {
                    log.info("Processando JWT token da URL: {}", maskToken(tokenParam));
                    processJwtToken(tokenParam, request);
                }
            } else {
                log.warn("Nenhum token de autenticação encontrado na requisição");
            }

            // Log de diagnóstico - autenticação
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.info("Usuário autenticado: {}, Autoridades: {}",
                        SecurityContextHolder.getContext().getAuthentication().getName(),
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            } else {
                log.warn("Nenhuma autenticação definida após processamento de token");
            }

        } catch (Exception e) {
            // Log detalhado, mas sem interromper o fluxo de filtros
            log.error("Erro ao processar autenticação: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private void processJwtToken(String token, HttpServletRequest request) {
        try {
            // Extrai o email do usuário
            String userEmail = jwtService.extractUsername(token);
            log.info("Email extraído do JWT: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Busca detalhes do usuário
                log.info("Buscando detalhes do usuário: {}", userEmail);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                log.info("Usuário encontrado: {}, Autoridades: {}", userDetails.getUsername(), userDetails.getAuthorities());

                // Verifica se o token existe no banco e é válido
                AccessTokenEntity tokenEntity = accessTokenPersistenceAdapter.findTokenByValue(token);
                log.info("Token encontrado no banco: {}", tokenEntity != null);

                boolean isTokenValid = false;

                if (tokenEntity != null) {
                    isTokenValid = !tokenEntity.isExpired() && !tokenEntity.isRevoked();
                    log.info("Token válido: {}, Expirado: {}, Revogado: {}",
                            isTokenValid, tokenEntity.isExpired(), tokenEntity.isRevoked());
                }

                // Valida o token JWT
                boolean isJwtValid = jwtService.isTokenValid(token, userDetails);
                log.info("JWT válido: {}", isJwtValid);

                if (isJwtValid && isTokenValid) {
                    setAuthentication(userDetails, request);
                    log.info("Autenticação JWT bem-sucedida para usuário: {}", userEmail);
                } else {
                    log.warn("Token JWT inválido para usuário: {}, JWT válido: {}, Token válido: {}",
                            userEmail, isJwtValid, isTokenValid);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar token JWT: {}", e.getMessage(), e);
        }
    }

    private void processApiToken(String token, HttpServletRequest request) {
        try {
            boolean isValid = apiTokenService.validateToken(token);
            log.info("API Token válido: {}", isValid);

            if (isValid) {
                Optional<ApiToken> apiToken = apiTokenService.getApiToken(token);
                log.info("API Token encontrado: {}", apiToken.isPresent());

                if (apiToken.isPresent()) {
                    boolean isActivated = apiToken.get().isActivated();
                    log.info("API Token ativo: {}", isActivated);

                    if (isActivated) {
                        String userEmail = apiToken.get().getUser().getEmail();
                        log.info("Email do usuário do API Token: {}", userEmail);

                        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                        log.info("Usuário encontrado: {}, Autoridades: {}",
                                userDetails.getUsername(), userDetails.getAuthorities());

                        setAuthentication(userDetails, request);
                        log.info("Autenticação API Token bem-sucedida para usuário: {}", userEmail);
                    } else {
                        log.warn("API Token está inativo: {}", maskUUID(token));
                    }
                } else {
                    log.warn("API Token não encontrado: {}", maskUUID(token));
                }
            } else {
                log.warn("API Token inválido: {}", maskUUID(token));
            }
        } catch (Exception e) {
            log.error("Erro ao processar API token: {}", e.getMessage(), e);
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.info("Autenticação definida no SecurityContext: {}", userDetails.getUsername());
    }

    private boolean isValidUUID(String token) {
        try {
            UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            log.trace("Token não é um UUID válido: {}", token);
            return false;
        }
    }

    /**
     * Registra todos os headers da requisição para diagnóstico
     */
    private void logAllHeaders(HttpServletRequest request) {
        log.info("--- Headers da requisição ---");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);

            // Oculta informações sensíveis
            if (headerName.equalsIgnoreCase("Authorization")) {
                headerValue = maskAuthHeader(headerValue);
            }

            log.info("Header: {} = {}", headerName, headerValue);
        }
        log.info("---------------------------");
    }

    /**
     * Mascara tokens para exibição segura nos logs
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***token muito curto***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }

    /**
     * Mascara UUIDs para exibição segura nos logs
     */
    private String maskUUID(String uuid) {
        if (uuid == null || uuid.length() < 10) {
            return "***uuid muito curto***";
        }
        return uuid.substring(0, 5) + "..." + uuid.substring(uuid.length() - 5);
    }

    /**
     * Mascara o header de Authorization para exibição segura nos logs
     */
    private String maskAuthHeader(String authHeader) {
        if (authHeader == null) {
            return null;
        }

        if (authHeader.startsWith("Bearer ")) {
            return "Bearer " + maskToken(authHeader.substring(7));
        } else if (isValidUUID(authHeader)) {
            return maskUUID(authHeader);
        }

        return "***authorization mascarada***";
    }
}