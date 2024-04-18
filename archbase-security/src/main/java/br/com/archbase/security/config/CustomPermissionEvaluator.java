package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.service.ArchbaseSecurityService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import java.io.Serializable;

public class CustomPermissionEvaluator implements PermissionEvaluator {
    private ArchbaseSecurityService securityService;

    public CustomPermissionEvaluator(ArchbaseSecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof MethodInvocation methodInvocation) {
            HasPermission hasPermission = methodInvocation.getMethod().getAnnotation(HasPermission.class);
            if (hasPermission != null) {
                String tenantId = hasPermission.tenantId().isEmpty() ? ArchbaseTenantContext.getTenantId() : hasPermission.tenantId();
                String companyId = hasPermission.companyId().isEmpty() ? ArchbaseTenantContext.getCompanyId() : hasPermission.companyId();
                return securityService.hasPermission(authentication, hasPermission.action(), hasPermission.resource(),
                        tenantId, companyId, hasPermission.projectId());
            }
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}