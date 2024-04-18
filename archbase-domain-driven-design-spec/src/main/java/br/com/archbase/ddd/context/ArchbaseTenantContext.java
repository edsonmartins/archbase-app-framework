package br.com.archbase.ddd.context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ArchbaseTenantContext {

    public static final String X_TENANT_ID = "X-TENANT-ID";
    public static final String X_COMPANY_ID = "X-COMPANY-ID";

    private ArchbaseTenantContext() {}

    private static final InheritableThreadLocal<String> currentTenant = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> currentCompany = new InheritableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        log.debug("Setting tenantId to " + tenantId);
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void setCompanyId(String companyId) {
        log.debug("Setting company to " + companyId);
        currentCompany.set(companyId);
    }

    public static String getCompanyId() {
        return currentCompany.get();
    }

    public static void clear(){
        currentTenant.remove();
        currentCompany.remove();
    }
}