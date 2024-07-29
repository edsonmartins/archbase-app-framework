package br.com.archbase.multitenancy;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ArchbaseCurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseCurrentTenantIdentifierResolverImpl.class);

    @Value("${archbase.app.tenant.default.id}")
    protected String tenantId;

    @PostConstruct
    public void init() {
        ArchbaseTenantContext.setTenantId(tenantId);
    }

    public ArchbaseCurrentTenantIdentifierResolverImpl() {
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String contextTenantId = ArchbaseTenantContext.getTenantId();
        return !ObjectUtils.isEmpty(contextTenantId) ? contextTenantId : this.getTenantId();
    }

    private String getTenantId() {
        if (tenantId == null) {
            tenantId = "archbase"; // Fallback value if somehow tenantId is not set
        }
        logger.info("Returning tenant ID: {}", tenantId);
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
