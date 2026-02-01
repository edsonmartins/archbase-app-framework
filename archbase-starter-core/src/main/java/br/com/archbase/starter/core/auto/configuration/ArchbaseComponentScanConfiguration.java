package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuração separada para component scanning compatível com Spring Boot 3.5.3+
 * Remove dependência de placeholders em anotações
 */
@Configuration
@ConditionalOnProperty(name = "archbase.web.mvc.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = {
    "br.com.archbase.ddd.infraestructure.aspect",
    "br.com.archbase.multitenancy", 
    "br.com.archbase.security",
    "br.com.archbase.web.config"
})
@EntityScan(basePackages = {
    "br.com.archbase.security.persistence"
})
@EnableJpaRepositories(
    basePackages = {"br.com.archbase.security.repository"},
    repositoryBaseClass = CommonArchbaseJpaRepository.class
)
@Import(ArchbaseAdditionalScanConfigurer.class)
public class ArchbaseComponentScanConfiguration {

    @Autowired
    private Environment environment;

    @Autowired  
    private ApplicationContext applicationContext;

    @PostConstruct
    public void logAdditionalPackages() {
        // Log packages adicionais configurados pelo usuário
        String componentScan = environment.getProperty("archbase.app.component.scan");
        String entities = environment.getProperty("archbase.app.jpa.entities");
        String repositories = environment.getProperty("archbase.app.jpa.repositories");
        
        if (StringUtils.hasText(componentScan)) {
            System.out.println("Archbase: Component scan adicional configurado: " + componentScan);
        }
        if (StringUtils.hasText(entities)) {
            System.out.println("Archbase: Entity scan adicional configurado: " + entities);
        }
        if (StringUtils.hasText(repositories)) {
            System.out.println("Archbase: Repository scan adicional configurado: " + repositories);
        }
    }
}