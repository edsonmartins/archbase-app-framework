package br.com.archbase.multitenancy;

import java.util.Map;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;


@Component
public class ArchbaseCurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {

    @Value("${archbase.app.tenant.default.id}")
    protected String tenantId = "archbase";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String contextTenantId = ArchbaseTenantContext.getTenantId();
        if (!ObjectUtils.isEmpty(contextTenantId)) {
            return contextTenantId;
        } else {
            return tenantId;
        }
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }

}
