package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.resource.logger.aspect.SimpleArchbaseResourceAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import jakarta.persistence.EntityManager;

import java.util.Optional;

/**
 * Configuração padrão do servidor MVC do Archbase Framework.
 * Esta configuração é carregada por padrão, mas pode ser desabilitada através de
 * propriedades ou sobrescrita por outra configuração com o nome 'customWebMvcConfigurer'.
 *
 * <p>Esta classe já declarou também {@code RepositoryRestConfigurer}, do Spring Data REST, sem
 * jamais sobrescrever um método dessa interface. O preço era alto: a declaração obrigava o
 * framework a depender de {@code spring-data-rest-webmvc}, que ia parar no classpath de toda
 * aplicação e arrastava junto o {@code spring-hateoas}. Bastava essa presença para o springdoc
 * ligar o {@code SpringDocDataRestConfiguration}, que no Spring Boot 4 exige autoconfigurações que
 * quase ninguém tem — e a aplicação não subia.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Import(ArchbaseComponentScanConfiguration.class)
@ConditionalOnMissingBean(name = "customWebMvcConfigurer")
@ConditionalOnProperty(name = "archbase.web.mvc.enabled", havingValue = "true", matchIfMissing = true)
public class ArchbaseServerMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    @Lazy
    private EntityManager entityManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public SimpleArchbaseResourceAspect genericControllerAspect() {
        return new SimpleArchbaseResourceAspect();
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Registra o interceptor de mudança de idioma
        registry.addInterceptor(localeChangeInterceptor());

        // Os interceptors com @ArchbaseInterceptor são registrados pelo ArchbaseInterceptorConfiguration, que vale com
        // ou sem esta configuração MVC.
    }

    /**
     * {@code @EnableJpaAuditing} acima aponta para o bean {@code auditorAware}, que só o {@code archbase-security}
     * declara. Sem o módulo, ou com {@code archbase.security.enabled=false}, o contexto não subia
     * ({@code No bean named 'auditorAware' available}). Aqui entra um que não atribui autor — a não ser que a
     * aplicação declare o seu com esse nome.
     */
    @Configuration(proxyBeanMethods = false)
    @Conditional(SemAuditorDaSeguranca.class)
    static class AuditorPadrao {
        @Bean
        @ConditionalOnMissingBean(name = "auditorAware")
        public AuditorAware<String> auditorAware() {
            return Optional::empty;
        }
    }

    static class SemAuditorDaSeguranca extends AnyNestedCondition {
        SemAuditorDaSeguranca() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnMissingClass("br.com.archbase.security.config.ArchbaseSecurityApplicationConfig")
        static class SemModuloDeSeguranca {
        }

        @ConditionalOnProperty(prefix = "archbase.security", name = "enabled", havingValue = "false")
        static class SegurancaDesligada {
        }
    }
}