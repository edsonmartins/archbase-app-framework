package br.com.archbase.error.handling;

import br.com.archbase.error.handling.handler.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

/**
 * @author edsonmartins
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ArchbaseErrorHandlingProperties.class)
@ConditionalOnProperty(value = "error.handling.enabled", matchIfMissing = true)
@PropertySource("classpath:/error-handling-defaults.properties")
public class ArchbaseErrorHandlingConfiguration {
    @Bean
    public ErrorHandlingControllerAdvice errorHandlingControllerAdvice(ArchbaseErrorHandlingProperties properties,
                                                                       List<ArchbaseApiExceptionHandler> handlers,
                                                                       FallbackApiExceptionHandler fallbackApiExceptionHandler) {
        return new ErrorHandlingControllerAdvice(properties,
                handlers,
                fallbackApiExceptionHandler);
    }

    @Bean
    public FallbackApiExceptionHandler defaultHandler(ArchbaseErrorHandlingProperties properties) {
        return new SimpleFallbackApiExceptionHandler(properties);
    }

    @Bean
    public TypeMismatchArchbaseApiExceptionHandler typeMismatchApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        return new TypeMismatchArchbaseApiExceptionHandler(properties);
    }

    @Bean
    public SpringValidationArchbaseApiExceptionHandler springValidationApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        return new SpringValidationArchbaseApiExceptionHandler(properties);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
    public SpringSecurityArchbaseApiExceptionHandler springSecurityApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        return new SpringSecurityArchbaseApiExceptionHandler(properties);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.orm.ObjectOptimisticLockingFailureException")
    public ObjectOptimisticLockingFailureArchbaseApiExceptionHandler objectOptimisticLockingFailureApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        return new ObjectOptimisticLockingFailureArchbaseApiExceptionHandler(properties);
    }
}
