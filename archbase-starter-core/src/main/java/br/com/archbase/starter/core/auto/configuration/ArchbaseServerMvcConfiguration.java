package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.resource.logger.aspect.SimpleArchbaseResourceAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import jakarta.persistence.EntityManager;

/**
 * Configuração padrão do servidor MVC do Archbase Framework.
 * Esta configuração é carregada por padrão, mas pode ser desabilitada através de
 * propriedades ou sobrescrita por outra configuração com o nome 'customWebMvcConfigurer'.
 */
@Configuration
@EnableWebMvc
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@ComponentScan(basePackages = {"br.com.archbase.ddd.infraestructure.aspect","br.com.archbase.multitenancy","br.com.archbase.security","${archbase.app.component.scan}","br.com.archbase.web.config"})
@EntityScan(basePackages = {"br.com.archbase.security.persistence","${archbase.app.jpa.entities}" })
@EnableJpaRepositories(
        basePackages = {"br.com.archbase.security.repository", "${archbase.app.jpa.repositories}"},
        repositoryBaseClass = CommonArchbaseJpaRepository.class)
@ConditionalOnMissingBean(name = "customWebMvcConfigurer")
@ConditionalOnProperty(name = "archbase.web.mvc.enabled", havingValue = "true", matchIfMissing = true)
public class ArchbaseServerMvcConfiguration implements WebMvcConfigurer, RepositoryRestConfigurer {

    @Autowired
    @Lazy
    private EntityManager entityManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private ArchbaseInterceptorRegister interceptorRegister;

    public ArchbaseServerMvcConfiguration() {
        // Como debug, vamos imprimir quando esta classe é carregada
        System.out.println("ArchbaseServerMvcConfiguration foi carregada");
    }

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

        // Registra os interceptors customizados marcados com @ArchbaseInterceptor
        if (interceptorRegister != null) {
            interceptorRegister.registerInterceptors(registry);
        }
    }
}