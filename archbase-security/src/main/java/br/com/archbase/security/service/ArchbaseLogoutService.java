package br.com.archbase.security.service;

import br.com.archbase.security.repository.AccessTokenJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArchbaseLogoutService implements LogoutHandler {

    private final AccessTokenJpaRepository tokenRepository;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        jwt = authHeader.substring(7);
        var storedToken = tokenRepository.findByToken(jwt)
                .orElse(null);
        if (storedToken == null) {
            return;
        }

        storedToken.setExpired(true);
        storedToken.setRevoked(true);
        tokenRepository.save(storedToken);

        // Revogar só o access token apresentado deixava o refresh do mesmo login intacto: bastava
        // trocá-lo em /auth/refresh-token para desfazer o logout. Como o login já revoga tudo do
        // usuário ao emitir um par novo, derrubar o conjunto aqui é o encerramento coerente da
        // sessão — e não há sessão paralela para preservar.
        if (storedToken.getUser() != null) {
            var remaining = tokenRepository.findAllValidTokensByUserId(storedToken.getUser().getId());
            remaining.forEach(token -> {
                token.setExpired(true);
                token.setRevoked(true);
            });
            tokenRepository.saveAll(remaining);
        }

        SecurityContextHolder.clearContext();
    }
}
