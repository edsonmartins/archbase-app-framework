package br.com.archbase.security.integration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Map;

/**
 * Aplicação mínima que sobe o módulo de segurança inteiro para os testes de integração.
 *
 * <p><b>Uma só, compartilhada por todos os testes.</b> O {@code @ComponentScan} varre
 * {@code br.com.archbase.security}, que também contém as classes de teste: duas configurações de
 * teste declarando o mesmo bean se encontram no scan uma da outra e o contexto falha com
 * {@code BeanDefinitionOverrideException}. Concentrar aqui elimina a colisão e garante que os
 * testes em H2, PostgreSQL e MySQL exercitem exatamente o mesmo contexto.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "br.com.archbase.security")
@EntityScan(basePackages = "br.com.archbase.security.persistence")
@EnableJpaRepositories(
        basePackages = "br.com.archbase.security.repository",
        repositoryBaseClass = CommonArchbaseJpaRepository.class)
public class ArchbaseSecurityTestApplication {

    public static final String TENANT = "tenant-teste";

    /**
     * As entidades de segurança usam {@code @TenantId}; sem um resolver o Hibernate não consegue
     * preencher o discriminador. O módulo archbase-multitenancy não é dependência daqui, então o
     * teste fornece o mínimo.
     */
    @Bean
    public CurrentTenantIdentifierResolver<String> tenantIdentifierResolver() {
        return new TestTenantResolver();
    }

    static class TestTenantResolver
            implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {
        @Override
        public String resolveCurrentTenantIdentifier() {
            return TENANT;
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
