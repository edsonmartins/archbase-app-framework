package br.com.archbase.spring.boot.auto.configuration;

import br.com.archbase.spring.boot.configuration.ArchbaseMethodSecurityConfiguration;
import br.com.archbase.spring.boot.configuration.ArchbaseServerMvcConfiguration;
import br.com.archbase.spring.boot.configuration.ArchbaseThreadConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(value = {ArchbaseServerMvcConfiguration.class, ArchbaseThreadConfiguration.class,
        ArchbaseMethodSecurityConfiguration.class})
public class ArchbaseSpringBootAutoConfiguration {
}
