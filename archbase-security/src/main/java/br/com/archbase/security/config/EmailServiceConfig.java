package br.com.archbase.security.config;

import br.com.archbase.security.service.ArchbaseEmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailServiceConfig {

    @Bean
    @ConditionalOnMissingBean(ArchbaseEmailService.class)
    public ArchbaseEmailService defaultArchbaseEmailService() {
        return new ArchbaseEmailService() {
            @Override
            public void sendResetPasswordEmail(String email, String resetPasswordToken, String userName, String name) {
                throw new UnsupportedOperationException("Por favor, forneça uma implementação de ArchbaseEmailService");
            }

            @Override
            public void sendActivationTokenApiEmail(String email, String activationLink, String userName, String name) {
                throw new UnsupportedOperationException("Por favor, forneça uma implementação de ArchbaseEmailService");
            }
        };
    }
}
