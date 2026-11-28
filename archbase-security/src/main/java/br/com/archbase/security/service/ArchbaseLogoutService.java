package br.com.archbase.security.service;

import br.com.archbase.security.repository.AccessTokenJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchbaseLogoutService implements LogoutHandler {

    private final AccessTokenJpaRepository tokenRepository;

    /** Trilha de segurança; ausente ou desligada, o logout não é registrado. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private br.com.archbase.security.audit.ArchbaseSecurityEventLogger eventLogger;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ArchbaseJwtService jwtService;

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

        // Revogar só o access token apresentado deixava o refresh do mesmo login intacto: bastava
        // trocá-lo em /auth/refresh-token para desfazer o logout. Como o login já revoga tudo do
        // usuário ao emitir um par novo, derrubar o conjunto aqui é o encerramento coerente da
        // sessão — e não há sessão paralela para preservar.
        //
        // Um único UPDATE resolve os dois pontos delicados: não navega a associação LAZY do usuário
        // (esta classe roda fora do OpenEntityManagerInView) e não passa por @Version, então um
        // refresh concorrente não derruba a revogação por conflito otimista.
        String ownerId = tokenRepository.findOwnerIdByToken(jwt).orElse(null);
        if (ownerId == null) {
            log.debug("Logout: token apresentado não corresponde a nenhuma sessão registrada");
            SecurityContextHolder.clearContext();
            return;
        }

        int revogados = tokenRepository.revokeAllTokensOfUser(ownerId);
        log.debug("Logout: {} token(s) revogado(s)", revogados);
        registrarNaTrilha(jwt, ownerId);

        SecurityContextHolder.clearContext();
    }

    /**
     * Até a 3.5.0 o tipo LOGOUT existia na trilha e nada o gravava. Usuário e tenant vêm do próprio token: o
     * logout roda antes do filtro JWT, sem autenticação nem tenant no contexto. Se o token não puder ser lido
     * (expirado, por exemplo), registra o id do dono da sessão.
     */
    private void registrarNaTrilha(String jwt, String ownerId) {
        if (eventLogger == null) {
            return;
        }
        String usuario = ownerId;
        String tenant = null;
        if (jwtService != null) {
            try {
                usuario = jwtService.extractUsername(jwt);
                tenant = jwtService.extractTenantId(jwt);
            } catch (Exception e) {
                log.debug("Logout: token não pôde ser lido para a trilha; registrando o id do dono", e);
            }
        }
        eventLogger.logout(usuario, "logout", tenant);
    }
}
