package br.com.archbase.multitenancy.interceptor;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ArchbaseTenantRequestInterceptor implements HandlerInterceptor {

    public static final String X_TENANT_ID = "X-TENANT-ID";
    public static final String X_COMPANY_ID = "X-COMPANY-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = request.getHeader(X_TENANT_ID);
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = request.getParameter(X_TENANT_ID);
        }

        String companyId = request.getHeader(X_COMPANY_ID);
        if (companyId == null || companyId.isEmpty()) {
            companyId = request.getParameter(X_COMPANY_ID);
        }

        if (tenantId != null && !tenantId.isEmpty()) {
            ArchbaseTenantContext.setTenantId(tenantId);
        }

        if (companyId != null && !companyId.isEmpty()) {
            ArchbaseTenantContext.setCompanyId(companyId);
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        ArchbaseTenantContext.clear();
    }
}
