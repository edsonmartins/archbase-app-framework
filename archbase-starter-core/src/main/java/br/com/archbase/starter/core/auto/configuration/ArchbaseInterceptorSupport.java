package br.com.archbase.starter.core.auto.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração que garante que os interceptors customizados serão registrados
 * mesmo quando o desenvolvedor define seu próprio WebMvcConfigurer.
 * 
 * Esta classe só é carregada quando há um WebMvcConfigurer personalizado
 * (diferente do ArchbaseServerMvcConfiguration) e fornece um bean adicional
 * que complementa a configuração do desenvolvedor.
 */
/**
 * @deprecated Nunca foi registrada (não está em nenhum {@code .imports} nem na varredura). O registro dos interceptors
 * com {@link ArchbaseInterceptor} é do {@link ArchbaseInterceptorConfiguration}, que vale com ou sem um
 * {@code WebMvcConfigurer} próprio da aplicação. Mantida só para não quebrar quem a referencia.
 */
@Deprecated(since = "3.5.0", forRemoval = true)
public class ArchbaseInterceptorSupport {

    @Autowired(required = false)
    private ArchbaseInterceptorRegister interceptorRegistrar;

    /**
     * Cria um WebMvcConfigurer adicional que apenas registra os interceptors Archbase.
     * Este configurer não interfere com outras configurações feitas pelo desenvolvedor.
     */
    @Bean
    public WebMvcConfigurer archbaseInterceptorConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                if (interceptorRegistrar != null) {
                    interceptorRegistrar.registerInterceptors(registry);
                }
            }
        };
    }
}