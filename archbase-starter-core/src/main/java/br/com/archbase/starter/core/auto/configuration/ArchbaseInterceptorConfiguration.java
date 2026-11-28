package br.com.archbase.starter.core.auto.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra no Spring MVC os beans anotados com {@link ArchbaseInterceptor}, com os padrões de caminho e a ordem da
 * anotação.
 *
 * <p>Vale com ou sem a configuração MVC do starter ({@code archbase.web.mvc.enabled}, {@code customWebMvcConfigurer}).
 * Até a 3.5.0 nada era registrado: o {@code ArchbaseInterceptorRegister} ficava fora de qualquer varredura e o
 * {@code ArchbaseInterceptorSupport}, que cobriria o caso do configurer próprio, não estava registrado.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
public class ArchbaseInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ArchbaseInterceptorRegister archbaseInterceptorRegister(ApplicationContext applicationContext) {
        return new ArchbaseInterceptorRegister(applicationContext);
    }

    @Bean
    public WebMvcConfigurer archbaseInterceptorConfigurer(ArchbaseInterceptorRegister register) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                register.registerInterceptors(registry);
            }
        };
    }
}
