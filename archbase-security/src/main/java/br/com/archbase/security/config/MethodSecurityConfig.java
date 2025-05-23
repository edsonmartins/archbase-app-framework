package br.com.archbase.security.config;

import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.util.CustomPointcutUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

    private final CustomAuthorizationManager customAuthorizationManager;

    public MethodSecurityConfig(CustomAuthorizationManager customAuthorizationManager) {
        this.customAuthorizationManager = customAuthorizationManager;
    }

    @Bean
    public AuthorizationManagerBeforeMethodInterceptor customAuthorizationManagerInterceptor() {
        return new AuthorizationManagerBeforeMethodInterceptor(
                CustomPointcutUtil.forAnnotations(HasPermission.class),
                customAuthorizationManager
        );
    }
}