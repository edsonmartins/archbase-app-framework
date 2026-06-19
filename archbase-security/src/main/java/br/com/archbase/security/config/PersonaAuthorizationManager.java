package br.com.archbase.security.config;

import br.com.archbase.security.annotations.RequirePersona;
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * AuthorizationManager para processar a anotação @RequirePersona.
 * 
 * Esta implementação valida personas de negócio e pode ser estendida
 * por enrichers específicos da aplicação para validações customizadas
 * baseadas no contexto da aplicação (STORE_APP, CUSTOMER_APP, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersonaAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    
    private final ArchbaseSecurityService securityService;
    
    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();
        RequirePersona requirePersona = method.getAnnotation(RequirePersona.class);
        
        if (requirePersona == null) {
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
            if (requirePersona.allowSystemAdmin() && user.getIsAdministrator() && user.isEnabled()) {
                log.debug("Acesso permitido para administrador do sistema: {}", user.getEmail());
                return new AuthorizationDecision(true);
            }
            
            // Verifica se usuário está ativo
            if (requirePersona.requireActiveUser() && !user.isEnabled()) {
                log.debug("Usuário inativo: {}", user.getEmail());
                return new AuthorizationDecision(false);
            }
            
            boolean hasAccess = validatePersonaAccess(user, requirePersona, auth);
            
            log.debug("Resultado da validação de persona para usuário {}: {}", 
                    user.getEmail(), hasAccess);
            
            return new AuthorizationDecision(hasAccess);
            
        } catch (Exception e) {
            log.error("Erro ao validar acesso por persona", e);
            return new AuthorizationDecision(false);
        }
    }
    
    /**
     * Valida acesso baseado em personas de negócio.
     * 
     * Esta implementação é básica e deve ser estendida por enrichers
     * específicos da aplicação que implementem lógica de validação
     * de personas baseada no contexto de negócio.
     */
    private boolean validatePersonaAccess(UserEntity user, RequirePersona requirePersona, Authentication auth) {
        List<String> requiredPersonas = Arrays.asList(requirePersona.value());
        
        log.debug("Validando personas: {} para usuário: {} no contexto: {}", 
                requiredPersonas, user.getEmail(), requirePersona.context());
        
        // Validação básica baseada no profile do usuário
        // Esta lógica pode ser estendida por enrichers da aplicação
        String userProfile = user.getProfile().getName();
        
        // Mapeamento básico de profiles para personas
        // Esta lógica deve ser customizada conforme o domínio
        boolean hasPersona = validateBasicPersonaMapping(userProfile, requiredPersonas, requirePersona);
        
        // Se não passou na validação básica, falha
        if (!hasPersona) {
            log.debug("Usuário não possui persona necessária");
            return false;
        }
        
        // Validação adicional de resource/action se especificados
        if (!requirePersona.resource().isEmpty()) {
            String resource = requirePersona.resource();
            String action = requirePersona.action().isEmpty() ? "READ" : requirePersona.action();
            
            log.debug("Validando permissão adicional - resource: {}, action: {}", resource, action);
            
            boolean hasPermission = securityService.hasPermission(auth, action, resource, null, null, null);
            if (!hasPermission) {
                log.debug("Usuário não possui permissão no resource: {}", resource);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Mapeamento básico de profiles para personas.
     * Esta lógica deve ser estendida/sobrescrita por enrichers específicos.
     */
    private boolean validateBasicPersonaMapping(String userProfile, List<String> requiredPersonas, RequirePersona annotation) {
        // Mapeamento básico - pode ser customizado
        for (String persona : requiredPersonas) {
            switch (persona) {
                case "PLATFORM_ADMIN":
                    if ("ADMIN".equals(userProfile) || "PLATFORM_ADMIN".equals(userProfile)) {
                        return true;
                    }
                    break;
                case "STORE_ADMIN":
                    if ("STORE_MANAGER".equals(userProfile) || "STORE_ADMIN".equals(userProfile)) {
                        return true;
                    }
                    break;
                case "CUSTOMER":
                    if ("CUSTOMER".equals(userProfile) || "USER".equals(userProfile)) {
                        return true;
                    }
                    break;
                case "DRIVER":
                    if ("DRIVER".equals(userProfile)) {
                        return true;
                    }
                    break;
                default:
                    // Para personas customizadas, verifica se o profile tem o mesmo nome
                    if (persona.equals(userProfile)) {
                        return true;
                    }
            }
        }
        
        // Se requireAll é true, precisa ter todas as personas
        if (annotation.requireAll()) {
            // Esta lógica precisa ser implementada baseada no contexto específico
            // Por enquanto, retorna false se não encontrou match exato
            return false;
        }
        
        return false; // Não encontrou nenhuma persona válida
    }
}