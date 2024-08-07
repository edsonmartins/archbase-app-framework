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
        String tenantId = request.getHeader(ArchbaseTenantContext.X_TENANT_ID);
        String companyId = request.getHeader(ArchbaseTenantContext.X_COMPANY_ID);
        ArchbaseTenantContext.setTenantId(tenantId);
        ArchbaseTenantContext.setCompanyId(companyId);
        if (request.getServletPath().contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                var isTokenValid = tokenRepository.findByToken(jwt)
                        .map(t -> !t.isExpired() && !t.isRevoked())
                        .orElse(false);
                if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } else if (isValidUUID(authHeader)) {
            if (apiTokenService.validateToken(authHeader)) {
                Optional<ApiToken> token = apiTokenService.getApiToken(authHeader);
                if (token.isEmpty() || !token.get().isActivated()) { // Verifica se o token foi ativado
                    throw new ArchbaseSecurityException("Token de API inválido ou não ativado.");
                }
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(token.get().getUser().getEmail());

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidUUID(String token) {
        try {
            UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
