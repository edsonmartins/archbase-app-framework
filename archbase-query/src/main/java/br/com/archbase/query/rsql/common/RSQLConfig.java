package br.com.archbase.query.rsql.common;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;

@Configuration
public class RSQLConfig {

    @Bean
    public RSQLSupport rsqlSupport(ApplicationContext applicationContext) {
        return new RSQLSupport(applicationContext.getBeansOfType(EntityManager.class));
    }

}
