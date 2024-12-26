package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.domain.entity.ApiToken;
import br.com.archbase.security.exception.ArchbaseSecurityException;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.service.ApiTokenService;
import br.com.archbase.security.service.ArchbaseJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("Antes do processamento: Authentication no SecurityContextHolder: {}",
                SecurityContextHolder.getContext().getAuthentication());
        log.debug("SecurityContextHolder: {}",
                SecurityContextHolder.getContextHolderStrategy().toString());

        String tenantId = request.getHeader(ArchbaseTenantContext.X_TENANT_ID);
        String companyId = request.getHeader(ArchbaseTenantContext.X_COMPANY_ID);
        if (StringUtils.isNotBlank(tenantId)) {
            ArchbaseTenantContext.setTenantId(tenantId);
        }
        if (StringUtils.isNotBlank(companyId)) {
            ArchbaseTenantContext.setCompanyId(companyId);
        }

        if (request.getServletPath().contains("/api/v1/auth")) {
            log.debug("Request to /api/v1/auth detected, bypassing authentication filter.");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            log.debug("Authorization header is missing. Skipping authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt;
        final String userEmail;

        if (authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.debug("Bearer token detected: {}", jwt);

            userEmail = jwtService.extractUsername(jwt);
            log.debug("Extracted username from JWT: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("User not authenticated. Validating token for user: {}", userEmail);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                var isTokenValid = tokenRepository.findByToken(jwt)
                        .map(t -> !t.isExpired() && !t.isRevoked())
                        .orElse(false);

                if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
                    log.debug("JWT is valid. Setting authentication for user: {}", userEmail);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("JWT is invalid or token is revoked/expired for user: {}", userEmail);
                }
            }
        } else if (isValidUUID(authHeader)) {
            log.debug("UUID token detected: {}", authHeader);
            if (apiTokenService.validateToken(authHeader)) {
                log.debug("UUID token validated successfully.");
                Optional<ApiToken> token = apiTokenService.getApiToken(authHeader);
                if (token.isEmpty() || !token.get().isActivated()) {
                    log.warn("Invalid or inactive API token: {}", authHeader);
                    throw new ArchbaseSecurityException("Token de API inválido ou não ativado.");
                }

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(token.get().getUser().getEmail());
                log.debug("Setting authentication for user: {}", userDetails.getUsername());
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.warn("UUID token validation failed: {}", authHeader);
            }
        } else {
            log.warn("Unrecognized authorization format: {}", authHeader);
        }


        log.debug("Antes do doFilter: Authentication no SecurityContextHolder: {}",
                SecurityContextHolder.getContext().getAuthentication());
        log.debug("SecurityContextHolder: {}",
                SecurityContextHolder.getContextHolderStrategy().toString());

        filterChain.doFilter(request, response);

        log.debug("Depois do doFilter do processamento: Authentication no SecurityContextHolder: {}",
                SecurityContextHolder.getContext().getAuthentication());
        log.debug("SecurityContextHolder: {}",
                SecurityContextHolder.getContextHolderStrategy().toString());
    }

    private boolean isValidUUID(String token) {
        try {
            UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid UUID format for token: {}", token);
            return false;
        }
    }
}

