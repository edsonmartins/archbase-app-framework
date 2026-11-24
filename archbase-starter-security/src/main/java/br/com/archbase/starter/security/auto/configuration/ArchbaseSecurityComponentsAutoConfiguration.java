package br.com.archbase.starter.security.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registra os componentes do {@code archbase-security} — serviços, filtro JWT, avaliadores, controllers, entidades e
 * repositórios — a partir do próprio starter.
 *
 * <p><b>O defeito que isto corrige.</b> Até a 3.5.0 quem varria {@code br.com.archbase.security} era o
 * {@code archbase-starter-core}, por uma configuração condicionada só a {@code archbase.web.mvc.enabled}. Três
 * consequências:
 * <ul>
 * <li>{@code archbase.security.enabled=false} não desligava nada: a varredura trazia as configurações de segurança
 * do mesmo jeito;</li>
 * <li>o {@code archbase-starter-security} sozinho não subia — a {@code DefaultArchbaseSecurityConfiguration} pedia o
 * filtro JWT, que só a varredura do core registrava;</li>
 * <li>{@code archbase.web.mvc.enabled=false} levava a segurança junto e o contexto não subia.</li>
 * </ul>
 *
 * <p><b>Quem já varre o framework.</b> Varrer o mesmo pacote duas vezes não duplica nada: o Spring descarta a
 * segunda definição idêntica. Já {@code @EnableJpaRepositories} duas vezes sobre o mesmo pacote derruba o contexto,
 * por isso os repositórios só são declarados aqui se a aplicação ainda não os declarou.
 *
 * <p><b>Entidades.</b> O pacote entra em {@link AutoConfigurationPackage}, que soma ao pacote da aplicação em vez de
 * substituí-lo, como faria um {@code @EntityScan}. Com o {@code archbase-starter-core} presente, vale o
 * {@code @EntityScan} dele, que já inclui o pacote.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "archbase.security", name = "enabled", matchIfMissing = true)
@ComponentScan(basePackages = "br.com.archbase.security")
@AutoConfigurationPackage(basePackages = "br.com.archbase.security.persistence")
public class ArchbaseSecurityComponentsAutoConfiguration {

    /**
     * Repositórios de segurança, a menos que a aplicação já os tenha declarado. Declarar aqui faz o Spring Boot
     * desistir da varredura automática de repositórios: sem o {@code archbase-starter-core}, que registra os da
     * aplicação por {@code archbase.app.jpa.repositories}, a aplicação declara os seus com
     * {@code @EnableJpaRepositories}.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(UserJpaRepository.class)
    @EnableJpaRepositories(
            basePackages = "br.com.archbase.security.repository",
            repositoryBaseClass = CommonArchbaseJpaRepository.class)
    static class Repositorios {
    }

}
