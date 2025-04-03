package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchbaseJwtAuthenticationFilter extends OncePerRequestFilter {

    private final ArchbaseJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AccessTokenJpaRepository tokenRepository;
    private final ApiTokenService apiTokenService;

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

        String tenantId = request.getHeader(X_TENANT_ID);
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = request.getParameter(X_TENANT_ID);
        }

        String companyId = request.getHeader(X_COMPANY_ID);
        if (companyId == null || companyId.isEmpty()) {
            companyId = request.getParameter(X_COMPANY_ID);
        }

        if (tenantId != null && !tenantId.isEmpty()) {
            ArchbaseTenantContext.setTenantId(tenantId);
        }

        if (companyId != null && !companyId.isEmpty()) {
            ArchbaseTenantContext.setCompanyId(companyId);
        }

        try {
            // Processar header de autorização
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    // Token JWT (usuário/senha)
                    String token = authHeader.substring(7);
                    processJwtToken(token, request);
                } else if (isValidUUID(authHeader)) {
                    // Token API (UUID direto)
                    processApiToken(authHeader, request);
                } else {
                    log.warn("Formato de autorização não reconhecido: {}", authHeader);
                }
            }
            // Se não tem header, tenta pegar token JWT da URL
            else if (tokenParam != null) {
                if (isValidUUID(tokenParam)) {
                    processApiToken(tokenParam, request);
                } else {
                    processJwtToken(tokenParam, request);
                }
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

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Busca detalhes do usuário
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Verifica se o token existe no banco e é válido
                Optional<AccessTokenEntity> tokenEntity = tokenRepository.findByToken(token);

                boolean isTokenValid = tokenEntity
                        .map(t -> !t.isExpired() && !t.isRevoked())
                        .orElse(false);

                // Valida o token JWT
                if (jwtService.isTokenValid(token, userDetails) && isTokenValid) {
                    setAuthentication(userDetails, request);
                    log.debug("Autenticação JWT bem-sucedida para usuário: {}", userEmail);
                } else {
                    log.debug("Token JWT inválido para usuário: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao processar token JWT: {}", e.getMessage());
        }
    }

    private void processApiToken(String token, HttpServletRequest request) {
        try {
            if (apiTokenService.validateToken(token)) {
                Optional<ApiToken> apiToken = apiTokenService.getApiToken(token);

                if (apiToken.isPresent() && apiToken.get().isActivated()) {
                    String userEmail = apiToken.get().getUser().getEmail();
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                    setAuthentication(userDetails, request);
                    log.debug("Autenticação API Token bem-sucedida para usuário: {}", userEmail);
                } else {
                    log.debug("API Token inativo ou não encontrado: {}", token);
                }
            } else {
                log.debug("API Token inválido: {}", token);
            }
        } catch (Exception e) {
            log.warn("Erro ao processar API token: {}", e.getMessage());
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
}