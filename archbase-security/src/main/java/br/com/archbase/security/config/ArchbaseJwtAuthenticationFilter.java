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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${archbase.app.tenant.default.id:}")
    private String defaultTenantId;

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

        try {
            log.debug("Requisição recebida: {} {}", request.getMethod(), request.getRequestURI());
            logAllHeaders(request);

            String tenantId = request.getHeader(X_TENANT_ID);
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = request.getParameter(X_TENANT_ID);
            }
            log.debug("TenantID recebido: {}", tenantId);

            String companyId = request.getHeader(X_COMPANY_ID);
            if (companyId == null || companyId.isEmpty()) {
                companyId = request.getParameter(X_COMPANY_ID);
            }
            log.debug("CompanyID recebido: {}", companyId);

            if (tenantId != null && !tenantId.isEmpty()) {
                ArchbaseTenantContext.setTenantId(tenantId);
                log.debug("TenantID definido no contexto: {}", tenantId);
            } else if (defaultTenantId != null && !defaultTenantId.isEmpty()) {
                ArchbaseTenantContext.setTenantId(defaultTenantId);
                log.debug("TenantID não fornecido na requisição, utilizando tenant padrão: {}", defaultTenantId);
            } else {
                log.debug("TenantID não fornecido na requisição e nenhum tenant padrão configurado (archbase.app.tenant.default.id)");
            }

            if (companyId != null && !companyId.isEmpty()) {
                ArchbaseTenantContext.setCompanyId(companyId);
                log.debug("CompanyID definido no contexto: {}", companyId);
            }

            log.debug("Authorization header presente: {}", authHeader != null);
            log.debug("Token parameter presente: {}", tokenParam != null);

            // Processar header de autorização
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    // Token JWT (usuário/senha)
                    String token = authHeader.substring(7);
                    log.debug("Processando Bearer token: {}", maskToken(token));
                    processJwtToken(token, request);
                } else if (isValidUUID(authHeader)) {
                    // Token API (UUID direto)
                    log.debug("Processando API token (UUID): {}", maskUUID(authHeader));
                    processApiToken(authHeader, request);
                } else {
                    log.warn("Formato de autorização não reconhecido: {}", maskAuthHeader(authHeader));
                }
            }
            // Se não tem header, tenta pegar token JWT da URL
            else if (tokenParam != null) {
                if (isValidUUID(tokenParam)) {
                    log.debug("Processando API token da URL (UUID): {}", maskUUID(tokenParam));
                    processApiToken(tokenParam, request);
                } else {
                    log.debug("Processando JWT token da URL: {}", maskToken(tokenParam));
                    processJwtToken(tokenParam, request);
                }
            } else {
                log.debug("Nenhum token de autenticação encontrado na requisição");
            }

            // Log de diagnóstico - autenticação
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("Usuário autenticado: {}, Autoridades: {}",
                        SecurityContextHolder.getContext().getAuthentication().getName(),
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            } else {
                log.debug("Nenhuma autenticação definida após processamento de token");
            }

            // Isolamento tenant↔token: o tenant do token (claim assinado) é a fonte de verdade.
            // Se o X-TENANT-ID do header divergir, rejeita com 403 (impede acesso cross-tenant).
            // Tokens legados (sem o claim) mantêm o comportamento anterior (fallback pelo header).
            String jwtForTenant = resolveJwtForTenant(authHeader, tokenParam);
            if (jwtForTenant != null && SecurityContextHolder.getContext().getAuthentication() != null) {
                String tokenTenant = jwtService.extractTenantId(jwtForTenant);
                if (tokenTenant != null && !tokenTenant.isEmpty()) {
                    if (tenantId != null && !tenantId.isEmpty() && !tenantId.equals(tokenTenant)) {
                        log.warn("Acesso cross-tenant NEGADO: usuario={}, tenantDoToken={}, X-TENANT-ID={}, {} {}",
                                SecurityContextHolder.getContext().getAuthentication().getName(),
                                tokenTenant, tenantId, request.getMethod(), request.getRequestURI());
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "X-TENANT-ID não corresponde ao tenant do token");
                        return;
                    }
                    // Fonte de verdade: alinha o contexto ao tenant do token.
                    ArchbaseTenantContext.setTenantId(tokenTenant);
                }
            }

            filterChain.doFilter(request, response);
        } catch (ServletException | IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Log detalhado, mas sem interromper o fluxo de filtros
            log.error("Erro ao processar autenticação: {}", e.getMessage(), e);
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    /** Retorna o JWT (Bearer ou via URL) para extração do claim de tenant; {@code null} para API token (UUID). */
    private String resolveJwtForTenant(String authHeader, String tokenParam) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (tokenParam != null && !isValidUUID(tokenParam)) {
            return tokenParam;
        }
        return null;
    }

    private void processJwtToken(String token, HttpServletRequest request) {
        try {
            // Extrai o email do usuário
            String userEmail = jwtService.extractUsername(token);
            log.debug("Email extraído do JWT: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Busca detalhes do usuário
                log.debug("Buscando detalhes do usuário: {}", userEmail);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                log.debug("Usuário encontrado: {}, Autoridades: {}", userDetails.getUsername(), userDetails.getAuthorities());

                // Verifica se o token existe no banco e é válido
                AccessTokenEntity tokenEntity = accessTokenPersistenceAdapter.findTokenByValue(token);
                log.debug("Token encontrado no banco: {}", tokenEntity != null);

                boolean isTokenValid = false;

                if (tokenEntity != null) {
                    isTokenValid = !tokenEntity.isExpired() && !tokenEntity.isRevoked();
                    log.debug("Token válido: {}, Expirado: {}, Revogado: {}",
                            isTokenValid, tokenEntity.isExpired(), tokenEntity.isRevoked());
                }

                // Valida o token JWT
                boolean isJwtValid = jwtService.isTokenValid(token, userDetails);
                log.debug("JWT válido: {}", isJwtValid);

                if (isJwtValid && isTokenValid) {
                    setAuthentication(userDetails, request);
                    log.debug("Autenticação JWT bem-sucedida para usuário: {}", userEmail);
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
            log.debug("API Token válido: {}", isValid);

            if (isValid) {
                Optional<ApiToken> apiToken = apiTokenService.getApiToken(token);
                log.debug("API Token encontrado: {}", apiToken.isPresent());

                if (apiToken.isPresent()) {
                    boolean isActivated = apiToken.get().isActivated();
                    log.debug("API Token ativo: {}", isActivated);

                    if (isActivated) {
                        String userEmail = apiToken.get().getUser().getEmail();
                        log.debug("Email do usuário do API Token: {}", userEmail);

                        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                        log.debug("Usuário encontrado: {}, Autoridades: {}",
                                userDetails.getUsername(), userDetails.getAuthorities());

                        setAuthentication(userDetails, request);
                        log.debug("Autenticação API Token bem-sucedida para usuário: {}", userEmail);
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
        log.debug("Autenticação definida no SecurityContext: {}", userDetails.getUsername());
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
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("--- Headers da requisição ---");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);

            // Oculta informações sensíveis
            if (headerName.equalsIgnoreCase("Authorization")) {
                headerValue = maskAuthHeader(headerValue);
            }

            log.debug("Header: {} = {}", headerName, headerValue);
        }
        log.debug("---------------------------");
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
