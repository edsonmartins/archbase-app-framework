package br.com.archbase.multitenancy;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class ArchbaseCurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseCurrentTenantIdentifierResolverImpl.class);

    private final TenantProperties tenantProperties;

    public ArchbaseCurrentTenantIdentifierResolverImpl(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
        this.init();
    }

    @PostConstruct
    public void init() {
        String defaultTenantId = tenantProperties.getId();
        ArchbaseTenantContext.setTenantId(defaultTenantId);
        logger.info("Default tenant ID initialized: {}", defaultTenantId);
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String contextTenantId = ArchbaseTenantContext.getTenantId();
        if (!ObjectUtils.isEmpty(contextTenantId)) {
            return contextTenantId;
        } else {
            return this.getTenantId();
        }
    }

    private String getTenantId() {
        String tenantId = tenantProperties.getId();
        if (ObjectUtils.isEmpty(tenantId)) {
            tenantId = "archbase"; // Fallback value
            logger.warn("Tenant ID is null, using fallback: {}", tenantId);
        }
        return tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.tenant_identifier_resolver", this);
    }
}
