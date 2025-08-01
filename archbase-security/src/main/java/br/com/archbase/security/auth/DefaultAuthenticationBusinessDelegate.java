package br.com.archbase.security.auth;

import br.com.archbase.security.domain.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Implementação padrão do AuthenticationBusinessDelegate.
 * Usada quando a aplicação não fornece sua própria implementação.
 * 
 * Esta implementação não adiciona nenhuma lógica de negócio específica,
 * apenas retorna os dados básicos de autenticação.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(AuthenticationBusinessDelegate.class)
public class DefaultAuthenticationBusinessDelegate implements AuthenticationBusinessDelegate {
    
    @Override
    public String onUserRegistered(User user, Map<String, Object> registrationData) {
        log.debug("Usuário registrado com implementação padrão: {}", user.getEmail());
        // Implementação básica - apenas retorna o ID do User
        return user.getId().toString();
    }
    
    @Override
    public AuthenticationResponse enrichAuthenticationResponse(
            AuthenticationResponse baseResponse, 
            String context, 
            HttpServletRequest request) {
        log.debug("Enriquecimento padrão - sem dados adicionais para contexto: {}", context);
        // Sem enriquecimento por padrão
        return baseResponse;
    }
    
    @Override
    public boolean supportsContext(String context) {
        // Suporta apenas contexto padrão
        return "DEFAULT".equals(context) || context == null;
    }
    
    @Override
    public List<String> getSupportedContexts() {
        return List.of("DEFAULT");
    }
    
    @Override
    public void preAuthenticate(String email, String context) {
        log.debug("Pre-autenticação padrão para: {} - contexto: {}", email, context);
        // Nenhuma validação adicional por padrão
    }
    
    @Override
    public void postAuthenticate(User user, String context) {
        log.debug("Pós-autenticação padrão para: {} - contexto: {}", user.getEmail(), context);
        // Nenhuma ação adicional por padrão
    }
    
    @Override
    public String getDefaultContext() {
        return "DEFAULT";
    }
}