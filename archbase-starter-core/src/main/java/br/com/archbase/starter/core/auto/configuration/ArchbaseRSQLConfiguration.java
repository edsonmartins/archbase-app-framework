package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.query.rsql.jpa.ArchbaseRSQLJPASupport;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArchbaseRSQLConfiguration {

    @Bean
    @ConditionalOnProperty(name = "archbase.rsql.enabled", matchIfMissing = true)
    public ArchbaseRSQLJPASupport rsqlSupport(ApplicationContext applicationContext) {
        return new ArchbaseRSQLJPASupport(applicationContext.getBeansOfType(EntityManager.class));
    }

}
