package br.com.archbase.security.config;

import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.annotations.RequireProfile;
import br.com.archbase.security.annotations.RequireRole;
import br.com.archbase.security.annotations.RequirePersona;
import br.com.archbase.security.util.CustomPointcutUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

    private final CustomAuthorizationManager customAuthorizationManager;
    private final ProfileAuthorizationManager profileAuthorizationManager;
    private final RoleAuthorizationManager roleAuthorizationManager;
    private final PersonaAuthorizationManager personaAuthorizationManager;

    public MethodSecurityConfig(CustomAuthorizationManager customAuthorizationManager,
                               ProfileAuthorizationManager profileAuthorizationManager,
                               RoleAuthorizationManager roleAuthorizationManager,
                               PersonaAuthorizationManager personaAuthorizationManager) {
        this.customAuthorizationManager = customAuthorizationManager;
        this.profileAuthorizationManager = profileAuthorizationManager;
        this.roleAuthorizationManager = roleAuthorizationManager;
        this.personaAuthorizationManager = personaAuthorizationManager;
    }

    @Bean
    public AuthorizationManagerBeforeMethodInterceptor customAuthorizationManagerInterceptor() {
        return new AuthorizationManagerBeforeMethodInterceptor(
                CustomPointcutUtil.forAnnotations(HasPermission.class),
                customAuthorizationManager
        );
    }
    
    @Bean
    public AuthorizationManagerBeforeMethodInterceptor profileAuthorizationManagerInterceptor() {
        return new AuthorizationManagerBeforeMethodInterceptor(
                CustomPointcutUtil.forAnnotations(RequireProfile.class),
                profileAuthorizationManager
        );
    }
    
    @Bean
    public AuthorizationManagerBeforeMethodInterceptor roleAuthorizationManagerInterceptor() {
        return new AuthorizationManagerBeforeMethodInterceptor(
                CustomPointcutUtil.forAnnotations(RequireRole.class),
                roleAuthorizationManager
        );
    }
    
    @Bean
    public AuthorizationManagerBeforeMethodInterceptor personaAuthorizationManagerInterceptor() {
        return new AuthorizationManagerBeforeMethodInterceptor(
                CustomPointcutUtil.forAnnotations(RequirePersona.class),
                personaAuthorizationManager
        );
    }
}