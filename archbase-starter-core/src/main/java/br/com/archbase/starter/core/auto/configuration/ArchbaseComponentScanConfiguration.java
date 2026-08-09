package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração separada para component scanning compatível com Spring Boot 3.5.3+
 *
 * Packages do framework são registrados via anotações estáticas.
 * Packages da aplicação do usuário são registrados dinamicamente via:
 * - archbase.app.component.scan (component scan)
 * - archbase.app.jpa.entities (entity scan)
 * - archbase.app.jpa.repositories (repository scan)
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
    "br.com.archbase.security.persistence",
    // A entidade de revisão da trilha de auditoria mora aqui. Sem este pacote, o Envers cai na
    // DefaultRevisionEntity e a trilha perde o usuário e o tenant — justamente o que ela existe
    // para registrar.
    "br.com.archbase.security.audit"
})
@EnableJpaRepositories(
    basePackages = {"br.com.archbase.security.repository"},
    repositoryBaseClass = CommonArchbaseJpaRepository.class
)
@Import({
    ArchbaseAdditionalScanConfigurer.class,
    ArchbaseDynamicJpaRepositoryConfigurer.class
})
public class ArchbaseComponentScanConfiguration {
}
