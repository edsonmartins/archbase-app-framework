package br.com.archbase.starter.core.auto.configuration;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Anotação para marcar interceptors que serão automaticamente registrados pelo Archbase Framework.
 * Este interceptor será adicionado na ordem especificada pelo atributo order.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ArchbaseInterceptor {
    
    /**
     * Define a ordem de execução do interceptor.
     * Interceptors com valores menores são executados primeiro.
     * @return a ordem de execução
     */
    int order() default Integer.MAX_VALUE;
    
    /**
     * Padrões de URL para aplicar este interceptor.
     * Se não for especificado, o interceptor será aplicado a todas as URLs.
     * @return padrões de URL
     */
    String[] pathPatterns() default {"/**"};
    
    /**
     * Padrões de URL para excluir deste interceptor.
     * @return padrões de URL para excluir
     */
    String[] excludePathPatterns() default {};
}