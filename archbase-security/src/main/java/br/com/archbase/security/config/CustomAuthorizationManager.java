package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.service.ArchbaseSecurityService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@Component
public class CustomAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    
    private final ArchbaseSecurityService securityService;
    
    public CustomAuthorizationManager(ArchbaseSecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();
        HasPermission hasPermission = method.getAnnotation(HasPermission.class);
        
        if (hasPermission == null) {
            // Se não tem a anotação, permite acesso (já que o pointcut só deve interceptar métodos com a anotação)
            return new AuthorizationDecision(true);
        }
        
        try {
            String tenantId = hasPermission.tenantId().isEmpty() ? 
                ArchbaseTenantContext.getTenantId() : hasPermission.tenantId();
            String companyId = hasPermission.companyId().isEmpty() ? 
                ArchbaseTenantContext.getCompanyId() : hasPermission.companyId();
            
            boolean hasAccess = securityService.hasPermission(
                authentication.get(), 
                hasPermission.action(), 
                hasPermission.resource(),
                tenantId, 
                companyId, 
                hasPermission.projectId()
            );
            
            return new AuthorizationDecision(hasAccess);
            
        } catch (Exception e) {
            // Log do erro se necessário
            return new AuthorizationDecision(false);
        }
    }
}