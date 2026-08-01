package br.com.archbase.security.service;

import br.com.archbase.security.repository.AccessTokenJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArchbaseLogoutService implements LogoutHandler {

    private final AccessTokenJpaRepository tokenRepository;

    /**
     * <b>Transacional de propósito.</b> Um {@link LogoutHandler} roda na cadeia de filtros do
     * Spring Security — fora do {@code OpenEntityManagerInView}, que só abre no interceptor do MVC.
     * Sem uma transação aqui, a entidade devolvida pelo repositório já vem desconectada e tocar o
     * {@code user} (associação LAZY) para descobrir o dono dos demais tokens estoura
     * {@code LazyInitializationException}: o logout falharia com 500 e o
     * {@code SecurityContextHolder.clearContext()} no fim nunca rodaria.
     */
    @Override
    @Transactional
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
