package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.service.ArchbaseSecurityService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@Component
public class CustomAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthorizationManager.class);

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
            // Falha ao AVALIAR a permissão não é o mesmo que "não tem permissão", mas negar é a
            // opção segura. O que não pode é negar em silêncio: sem este log, um erro de consulta ou
            // um principal inesperado viram um 403 idêntico ao de falta de permissão, e a
            // investigação vai parar no cadastro de permissões — que está correto.
            log.error("Erro ao avaliar permissão action={} resource={}: {}",
                    hasPermission.action(), hasPermission.resource(), e.getMessage(), e);
            return new AuthorizationDecision(false);
        }
    }
}