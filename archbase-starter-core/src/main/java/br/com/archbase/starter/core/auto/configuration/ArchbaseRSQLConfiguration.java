package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.query.rsql.jpa.ArchbaseRSQLJPASupport;
import br.com.archbase.query.rsql.common.RSQLCommonSupport;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArchbaseRSQLConfiguration {

    /**
     * Tamanho máximo de página aceito pelos endpoints de consulta (anti-DoS). Padrão {@code 1000};
     * um valor menor ou igual a zero desativa o limite.
     */
    @Value("${archbase.rsql.max-page-size:1000}")
    private int maxPageSize;

    @Bean
    @ConditionalOnProperty(name = "archbase.rsql.enabled", matchIfMissing = true)
    public ArchbaseRSQLJPASupport rsqlSupport(ApplicationContext applicationContext) {
        ArchbaseRSQLJPASupport support = new ArchbaseRSQLJPASupport(applicationContext.getBeansOfType(EntityManager.class));
        RSQLCommonSupport.setMaxPageSize(maxPageSize);
        return support;
    }

    @PreDestroy
    public void clearRsqlGlobalState() {
        RSQLCommonSupport.clear();
    }

}
