package br.com.archbase.starter.core.auto.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registrador automático de interceptors marcados com a anotação {@link ArchbaseHandlerInterceptor}.
 * Esta classe é responsável por encontrar todos os beans anotados e registrá-los no InterceptorRegistry.
 *
 * Esta classe será carregada como um componente Spring, independentemente se o desenvolvedor
 * definiu sua própria implementação de WebMvcConfigurer, garantindo que os interceptors
 * serão sempre registrados.
 */
@Component
@Order(0)
public class ArchbaseInterceptorRegister {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseInterceptorRegister.class);

    private final ApplicationContext applicationContext;

    @Autowired
    public ArchbaseInterceptorRegister(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Registra todos os interceptors anotados com {@link ArchbaseInterceptor} no registry fornecido.
     *
     * @param registry O registry onde os interceptors serão registrados
     */
    public void registerInterceptors(InterceptorRegistry registry) {
        // Encontrar todos os beans anotados com @ArchbaseInterceptor
        Map<String, Object> interceptors = applicationContext.getBeansWithAnnotation(ArchbaseInterceptor.class);

        // Filtrar apenas os que implementam HandlerInterceptor
        List<Object> validInterceptors = new ArrayList<>();

        for (Map.Entry<String, Object> entry : interceptors.entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof HandlerInterceptor) {
                validInterceptors.add(bean);
            } else {
                logger.warn("Bean '{}' tem anotação @ArchbaseInterceptor mas não implementa HandlerInterceptor",
                        entry.getKey());
            }
        }

        // Ordenar os interceptors com base no atributo 'order'
        validInterceptors.sort((o1, o2) -> {
            ArchbaseInterceptor a1 = o1.getClass().getAnnotation(ArchbaseInterceptor.class);
            ArchbaseInterceptor a2 = o2.getClass().getAnnotation(ArchbaseInterceptor.class);
            return Integer.compare(a1.order(), a2.order());
        });

        // Registrar cada interceptor com suas configurações
        for (Object interceptor : validInterceptors) {
            ArchbaseInterceptor annotation = interceptor.getClass().getAnnotation(ArchbaseInterceptor.class);

            InterceptorRegistration registration = registry.addInterceptor((HandlerInterceptor) interceptor);

            // Aplicar padrões de URL
            if (annotation.pathPatterns().length > 0) {
                registration.addPathPatterns(annotation.pathPatterns());
            }

            // Aplicar padrões de exclusão
            if (annotation.excludePathPatterns().length > 0) {
                registration.excludePathPatterns(annotation.excludePathPatterns());
            }

            logger.info("Registrado interceptor '{}' com ordem {}",
                    interceptor.getClass().getName(), annotation.order());
        }
    }
}