package br.com.archbase.security.integration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.security.auth.ArchbaseTenantInfoResolver;
import br.com.archbase.security.auth.TenantLoginOption;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    /**
     * Rótulo de tenant como uma aplicação real forneceria — ligado por propriedade porque a maior
     * parte dos testes precisa justamente do cenário <b>sem</b> resolver, em que o framework devolve
     * só o id. Um {@code @TestConfiguration} aninhado não serviria: o {@code @ComponentScan} acima
     * varre as classes de teste e o bean vazaria para o contexto de todos os outros cenários.
     */
    @Bean
    @ConditionalOnProperty(name = "teste.tenant-info-resolver.enabled", havingValue = "true")
    public ArchbaseTenantInfoResolver tenantInfoResolverDeTeste() {
        return tenantId -> TenantLoginOption.builder()
                .nome("Frigocenter")
                .descricao("Frigocenter Distribuidora Ltda")
                .build();
    }

    static class TestTenantResolver
            implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {
        /**
         * Lê o {@code ArchbaseTenantContext}, com o tenant de teste como padrão.
         *
         * <p>Devolvia {@code TENANT} fixo, e isso tornava o harness incapaz de exercitar qualquer
         * comportamento multi-tenant: o discriminador era o mesmo em toda consulta, houvesse ou não
         * contexto. Foi por isso que a inércia do logout em tenant não-padrão passou por 246 testes
         * sem nenhum acusar — o cenário era irreproduzível aqui.
         */
        @Override
        public String resolveCurrentTenantIdentifier() {
            String doContexto = br.com.archbase.ddd.context.ArchbaseTenantContext.getTenantId();
            return doContexto != null && !doContexto.isBlank() ? doContexto : TENANT;
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
