package br.com.archbase.security.config;

import br.com.archbase.security.annotations.RequireProfile;
import br.com.archbase.security.service.ArchbaseSecurityService;
import br.com.archbase.security.persistence.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * AuthorizationManager para processar a anotação @RequireProfile.
 * Segue o mesmo padrão do CustomAuthorizationManager existente no Archbase.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    
    private final ArchbaseSecurityService securityService;
    
    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();
        RequireProfile requireProfile = method.getAnnotation(RequireProfile.class);
        
        if (requireProfile == null) {
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
            if (requireProfile.allowSystemAdmin() && user.getIsAdministrator() && user.isEnabled()) {
                log.debug("Acesso permitido para administrador do sistema: {}", user.getEmail());
                return new AuthorizationDecision(true);
            }
            
            // Verifica se usuário está ativo
            if (requireProfile.requireActiveUser() && !user.isEnabled()) {
                log.debug("Usuário inativo: {}", user.getEmail());
                return new AuthorizationDecision(false);
            }
            
            boolean hasAccess = validateProfileAccess(user, requireProfile, auth);
            
            log.debug("Resultado da validação de profile para usuário {}: {}", 
                    user.getEmail(), hasAccess);
            
            return new AuthorizationDecision(hasAccess);
            
        } catch (Exception e) {
            log.error("Erro ao validar acesso por profile", e);
            return new AuthorizationDecision(false);
        }
    }
    
    /**
     * Valida se o usuário tem os profiles necessários.
     */
    private boolean validateProfileAccess(UserEntity user, RequireProfile requireProfile, Authentication auth) {
        List<String> requiredProfiles = Arrays.asList(requireProfile.value());
        
        // Busca os profiles do usuário através das UserProfiles
        List<String> userProfiles = new ArrayList<>();
        userProfiles.add(user.getProfile().getName());
        
        log.debug("Profiles necessários: {} | Profiles do usuário: {}", requiredProfiles, userProfiles);
        
        boolean hasProfile;
        if (requireProfile.requireAll()) {
            // Usuário deve ter TODOS os profiles
            hasProfile = userProfiles.containsAll(requiredProfiles);
        } else {
            // Usuário deve ter PELO MENOS UM dos profiles
            hasProfile = requiredProfiles.stream().anyMatch(userProfiles::contains);
        }
        
        // Se não tem o profile necessário, falha imediatamente
        if (!hasProfile) {
            return false;
        }
        
        // Validação adicional de resource/action se especificados
        if (!requireProfile.resource().isEmpty()) {
            String resource = requireProfile.resource();
            String action = requireProfile.action().isEmpty() ? "READ" : requireProfile.action();
            
            log.debug("Validando permissão adicional - resource: {}, action: {}", resource, action);
            
            return securityService.hasPermission(auth, action, resource, null, null, null);
        }
        
        return true;
    }
}