package br.com.archbase.security.adapter;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.usecase.SystemUserContext;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Implementação do contexto de usuário do sistema.
 * Permite executar operações como um usuário específico do sistema,
 * útil para webhooks, jobs, processamento de filas, etc.
 *
 * <p>Exemplo de uso com callback (recomendado):</p>
 * <pre>{@code
 * @Autowired
 * private SystemUserContext systemUserContext;
 *
 * public void processWebhook(WebhookPayload payload) {
 *     systemUserContext.runAsSystemUser("sistema@empresa.com", () -> {
 *         // Operações que requerem usuário autenticado
 *         orderService.processOrder(payload.getOrderId());
 *     });
 * }
 * }</pre>
 *
 * <p>Exemplo de uso manual:</p>
 * <pre>{@code
 * try {
 *     systemUserContext.setSystemUserContext("sistema@empresa.com");
 *     // Operações que requerem usuário autenticado
 *     orderService.processOrder(orderId);
 * } finally {
 *     systemUserContext.clearSystemUserContext();
 * }
 * }</pre>
 */
@Component
public class ArchbaseSystemUserContext implements SystemUserContext {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseSystemUserContext.class);

    private static final String SYSTEM_USER_ATTRIBUTE = "ARCHBASE_SYSTEM_USER_CONTEXT";

    @Autowired
    private UserJpaRepository userJpaRepository;

    private final ThreadLocal<Authentication> previousAuthentication = new ThreadLocal<>();

    @Override
    public <T> T runAsSystemUser(String systemUserEmail, Supplier<T> operation) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            setSystemUserContextInternal(systemUserEmail, true);
            return operation.get();
        } finally {
            restoreOrClearContext(previousAuth);
        }
    }

    @Override
    public void runAsSystemUser(String systemUserEmail, Runnable operation) {
        runAsSystemUser(systemUserEmail, () -> {
            operation.run();
            return null;
        });
    }

    @Override
    public void setSystemUserContext(String systemUserEmail) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        previousAuthentication.set(currentAuth);
        setSystemUserContextInternal(systemUserEmail, false);
    }

    @Override
    public void clearSystemUserContext() {
        Authentication previousAuth = previousAuthentication.get();
        restoreOrClearContext(previousAuth);
        previousAuthentication.remove();
    }

    @Override
    public boolean isSystemUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            Object details = authentication.getDetails();
            if (details instanceof SystemUserDetails) {
                return ((SystemUserDetails) details).isSystemUser();
            }
        }
        return false;
    }

    private void setSystemUserContextInternal(String systemUserEmail, boolean isCallback) {
        UserEntity systemUser = userJpaRepository.findByEmail(systemUserEmail)
                .orElseThrow(() -> new ArchbaseValidationException(
                        String.format("Usuário do sistema não encontrado: %s", systemUserEmail)));

        if (systemUser.getAccountDeactivated() || systemUser.getAccountLocked()) {
            throw new ArchbaseValidationException(
                    String.format("Usuário do sistema está desativado ou bloqueado: %s", systemUserEmail));
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                systemUser,
                null,
                systemUser.getAuthorities()
        );

        authToken.setDetails(new SystemUserDetails(true, systemUserEmail));

        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.info("Contexto de usuário do sistema definido: {} (callback: {})", systemUserEmail, isCallback);
    }

    private void restoreOrClearContext(Authentication previousAuth) {
        if (previousAuth != null) {
            SecurityContextHolder.getContext().setAuthentication(previousAuth);
            log.debug("Contexto de segurança restaurado para autenticação anterior");
        } else {
            SecurityContextHolder.clearContext();
            log.debug("Contexto de segurança limpo");
        }
    }

    /**
     * Classe interna para marcar detalhes de autenticação como contexto de usuário do sistema.
     */
    public static class SystemUserDetails {
        private final boolean systemUser;
        private final String systemUserEmail;

        public SystemUserDetails(boolean systemUser, String systemUserEmail) {
            this.systemUser = systemUser;
            this.systemUserEmail = systemUserEmail;
        }

        public boolean isSystemUser() {
            return systemUser;
        }

        public String getSystemUserEmail() {
            return systemUserEmail;
        }
    }
}
