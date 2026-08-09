package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.resource.logger.aspect.SimpleArchbaseResourceAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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

    @Autowired(required = false)
    private ArchbaseInterceptorRegister interceptorRegister;

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