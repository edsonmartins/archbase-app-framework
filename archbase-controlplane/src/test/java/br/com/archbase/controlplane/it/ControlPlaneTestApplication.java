package br.com.archbase.controlplane.it;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Map;

/**
 * Aplicação mínima com archbase-security e o módulo do Control Plane, como um produto a montaria. O módulo entra
 * pela autoconfiguração (não pelo component scan), como entraria numa aplicação real.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"br.com.archbase.security", "br.com.archbase.controlplane.it"})
@EntityScan(basePackages = "br.com.archbase.security.persistence")
@EnableJpaRepositories(
        basePackages = "br.com.archbase.security.repository",
        repositoryBaseClass = CommonArchbaseJpaRepository.class)
@EnableJpaAuditing
public class ControlPlaneTestApplication {

    public static final String DEFAULT_TENANT = "tenant-padrao";

    @Bean
    public CurrentTenantIdentifierResolver<String> tenantIdentifierResolver() {
        return new TestTenantResolver();
    }

    static class TestTenantResolver
            implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {
        @Override
        public String resolveCurrentTenantIdentifier() {
            String tenant = ArchbaseTenantContext.getTenantId();
            return tenant != null && !tenant.isBlank() ? tenant : DEFAULT_TENANT;
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }

        @Override
        public void customize(Map<String, Object> hibernateProperties) {
            hibernateProperties.put("hibernate.tenant_identifier_resolver", this);
        }
    }
}
