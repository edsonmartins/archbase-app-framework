package br.com.archbase.security.config;

import br.com.archbase.security.annotations.RequireRole;
import br.com.archbase.security.persistence.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * AuthorizationManager para processar a anotação @RequireRole.
 * 
 * Esta implementação fornece validação básica e pode ser estendida
 * por enrichers específicos da aplicação para validações customizadas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    
    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        
        if (requireRole == null) {
            return new AuthorizationDecision(true);
        }
        
        try {
            Authentication auth = authentication.get();
            
            if (auth == null || !auth.isAuthenticated()) {
                log.debug("Usuário não autenticado - acesso negado");
                return new AuthorizationDecision(false);
            }
            
            UserEntity user = (UserEntity) auth.getPrincipal();
            
            // Permite bypass para administradores do sistema
            if (requireRole.allowSystemAdmin() && user.getIsAdministrator() && user.isEnabled()) {
                log.debug("Acesso permitido para administrador do sistema: {}", user.getEmail());
                return new AuthorizationDecision(true);
            }
            
            // Verificação de admin da plataforma
            if (requireRole.requirePlatformAdmin() && (!user.getIsAdministrator() || !user.isEnabled())) {
                log.debug("Acesso negado - usuário não é admin da plataforma: {}", user.getEmail());
                return new AuthorizationDecision(false);
            }
            
            boolean hasAccess = validateRoleAccess(user, requireRole);
            
            log.debug("Resultado da validação de role para usuário {}: {}", 
                    user.getEmail(), hasAccess);
            
            return new AuthorizationDecision(hasAccess);
            
        } catch (Exception e) {
            log.error("Erro ao validar acesso por role", e);
            return new AuthorizationDecision(false);
        }
    }
    
    /**
     * Valida acesso baseado em roles customizadas.
     * 
     * Esta implementação é básica e pode ser estendida por enrichers
     * específicos da aplicação que implementem lógica de validação
     * de roles customizadas.
     */
    private boolean validateRoleAccess(UserEntity user, RequireRole requireRole) {
        List<String> requiredRoles = Arrays.asList(requireRole.value());
        
        log.debug("Validando roles customizadas: {} para usuário: {}", 
                requiredRoles, user.getEmail());
        
        // Implementação básica - pode ser sobrescrita por enrichers
        // Por padrão, administradores têm acesso a qualquer role
        if (user.getIsAdministrator() && user.isEnabled()) {
            log.debug("Acesso permitido - usuário é administrador");
            return true;
        }
        
        // Aqui poderia ser implementada lógica específica baseada em:
        // - Dados de contexto (ex: store, tenant)
        // - Roles customizadas armazenadas em outras tabelas
        // - Integração com enrichers da aplicação
        
        // Por enquanto, permite acesso para usuários ativos
        // Esta lógica deve ser customizada conforme necessário
        boolean hasAccess = user.isEnabled();
        
        log.debug("Validação de roles customizadas - acesso: {} para usuário: {}", 
                hasAccess, user.getEmail());
        
        return hasAccess;
    }
}