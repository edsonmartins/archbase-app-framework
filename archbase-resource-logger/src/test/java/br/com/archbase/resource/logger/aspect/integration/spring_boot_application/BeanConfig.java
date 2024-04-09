package br.com.archbase.resource.logger.aspect.integration.spring_boot_application;

import br.com.archbase.resource.logger.aspect.SimpleArchbaseResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan("br.com.archbase.resource.logger")
public class BeanConfig {
    @Bean
    public SimpleArchbaseResourceAspect genericControllerAspect() {
        return new SimpleArchbaseResourceAspect(null);
    }
}
