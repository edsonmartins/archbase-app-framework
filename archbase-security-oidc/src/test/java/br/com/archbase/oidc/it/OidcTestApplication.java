package br.com.archbase.oidc.it;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Aplicação mínima com archbase-security e o módulo OIDC, como uma aplicação real a montaria: o módulo entra pela
 * autoconfiguração, não pelo component scan.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"br.com.archbase.security", "br.com.archbase.oidc.it"})
@EntityScan(basePackages = "br.com.archbase.security.persistence")
@EnableJpaRepositories(
        basePackages = "br.com.archbase.security.repository",
        repositoryBaseClass = CommonArchbaseJpaRepository.class)
@EnableJpaAuditing
public class OidcTestApplication {

    public static final String TENANT = "tenant-teste";

    @Bean
    public CurrentTenantIdentifierResolver<String> tenantIdentifierResolver() {
        return new TestTenantResolver();
    }

    /** Devolve quem a requisição autenticou e em qual tenant — só exige estar autenticado. */
    @RestController
    public static class WhoAmIController {
        @GetMapping("/teste/eu")
        public Map<String, Object> eu(Authentication authentication) {
            UserEntity user = (UserEntity) authentication.getPrincipal();
            return Map.of(
                    "email", user.getEmail(),
                    "tipo", authentication.getClass().getSimpleName(),
                    "tenant", String.valueOf(ArchbaseTenantContext.getTenantId()));
        }
    }

    static class TestTenantResolver
            implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {
        @Override
        public String resolveCurrentTenantIdentifier() {
            String tenant = ArchbaseTenantContext.getTenantId();
            return tenant != null && !tenant.isBlank() ? tenant : TENANT;
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
