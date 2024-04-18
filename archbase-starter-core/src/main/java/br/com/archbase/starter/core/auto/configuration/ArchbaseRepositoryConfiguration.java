package br.com.archbase.starter.core.auto.configuration;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(
        repositoryBaseClass = CommonArchbaseJpaRepository.class,basePackages = {
        "br.com.relevant.erp.app.infrastructure.output.persistence.repository"})
public class ArchbaseRepositoryConfiguration {
}
