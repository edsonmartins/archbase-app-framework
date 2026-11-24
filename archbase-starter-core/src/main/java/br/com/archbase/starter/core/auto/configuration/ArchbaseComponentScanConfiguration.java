package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
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
 *
 * <p><b>Segurança.</b> Até a 3.5.0 esta classe também varria {@code br.com.archbase.security} e declarava os
 * repositórios de segurança. Isso passou para o {@code archbase-starter-security}
 * ({@code ArchbaseSecurityComponentsAutoConfiguration}), que respeita {@code archbase.security.enabled} e não depende
 * de {@code archbase.web.mvc.enabled}. Fica aqui só o caminho de quem usa este starter com o {@code archbase-security}
 * sem o starter de segurança: {@link SegurancaSemStarter}.
 */
@Configuration
@ConditionalOnProperty(name = "archbase.web.mvc.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = {
    "br.com.archbase.ddd.infraestructure.aspect",
    "br.com.archbase.multitenancy"
})
// As entidades da trilha ficam neste mesmo pacote, e não num "audit" separado, de propósito: há
// aplicações que montam o EntityManagerFactory à mão e listam os pacotes uma a uma. Um pacote novo
// obrigaria cada uma delas a acrescentar a linha — e quem esquecesse não subiria mais, mesmo com a
// auditoria desligada.
@EntityScan(basePackages = {
    "br.com.archbase.security.persistence"
})
@Import({
    ArchbaseAdditionalScanConfigurer.class,
    ArchbaseDynamicJpaRepositoryConfigurer.class
})
public class ArchbaseComponentScanConfiguration {

    /**
     * {@code archbase-security} no classpath sem o {@code archbase-starter-security}: varre o módulo como antes da
     * 3.5.0, agora respeitando {@code archbase.security.enabled}. Com o starter presente, quem registra é ele.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter")
    @ConditionalOnMissingClass("br.com.archbase.starter.security.auto.configuration.ArchbaseSecurityComponentsAutoConfiguration")
    @ConditionalOnProperty(prefix = "archbase.security", name = "enabled", matchIfMissing = true)
    @ComponentScan(basePackages = "br.com.archbase.security")
    static class SegurancaSemStarter {

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnMissingBean(type = "br.com.archbase.security.repository.UserJpaRepository")
        @EnableJpaRepositories(
            basePackages = {"br.com.archbase.security.repository"},
            repositoryBaseClass = CommonArchbaseJpaRepository.class
        )
        static class Repositorios {
        }
    }
}
