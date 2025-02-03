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
        String token = null;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                // Token JWT (usuário/senha)
                token = authHeader.substring(7);
                processJwtToken(token, request);
            } else if (isValidUUID(authHeader)) {
                // Token API (UUID direto)
                processApiToken(authHeader, request);
            } else {
                log.warn("Formato de autorização não reconhecido: {}", authHeader);
            }
        } else {
            // Se não tem header, tenta pegar token JWT da URL
            String tokenParam = request.getParameter("token");
            if (tokenParam != null) {
                token = tokenParam;
                processJwtToken(token, request);
            }
        }

        filterChain.doFilter(request, response);
    }


    private void processJwtToken(String token, HttpServletRequest request) {
        // Processo JWT token
        String userEmail = jwtService.extractUsername(token);
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            var isTokenValid = tokenRepository.findByToken(token)
                    .map(t -> !t.isExpired() && !t.isRevoked())
                    .orElse(false);

            if (jwtService.isTokenValid(token, userDetails) && isTokenValid) {
                setAuthentication(userDetails, request);
            }
        }
        // Processo API token
        else if (isValidUUID(token)) {
            processApiToken(token, request);
        }
    }

    private void processApiToken(String token, HttpServletRequest request) {
        if (apiTokenService.validateToken(token)) {
            Optional<ApiToken> apiToken = apiTokenService.getApiToken(token);
            if (apiToken.isPresent() && apiToken.get().isActivated()) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(
                        apiToken.get().getUser().getEmail()
                );
                setAuthentication(userDetails, request);
            }
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
            log.debug("Invalid UUID format for token: {}", token);
            return false;
        }
    }
}

