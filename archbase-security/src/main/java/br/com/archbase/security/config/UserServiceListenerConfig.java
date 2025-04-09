package br.com.archbase.security.config;

import br.com.archbase.security.service.ArchbaseEmailService;
import br.com.archbase.security.service.DefaultUserServiceListener;
import br.com.archbase.security.service.UserServiceListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserServiceListenerConfig {

    @Bean
    @ConditionalOnMissingBean(UserServiceListener.class)
    public UserServiceListener defaultUserServiceListener() {
        return new DefaultUserServiceListener();
    }
}
