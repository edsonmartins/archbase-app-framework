package br.com.archbase.starter.security.auto.configuration;

import br.com.archbase.security.config.ArchbaseSecurityApplicationConfig;
import br.com.archbase.security.config.DefaultArchbaseSecurityConfiguration;
import br.com.archbase.security.config.MethodSecurityConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "archbase.security", name = "enabled", matchIfMissing = true)
@Import({
        ArchbaseSecurityApplicationConfig.class,
        DefaultArchbaseSecurityConfiguration.class,
        MethodSecurityConfig.class
})
public class ArchbaseSecurityAutoConfiguration {
    
}
