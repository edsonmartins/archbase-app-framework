package br.com.archbase.starter.auto.configuration;


import br.com.archbase.starter.core.auto.configuration.ArchbaseCoreAutoConfiguration;
import br.com.archbase.starter.multitenancy.auto.configuration.ArchbaseMultitenancyAutoConfiguration;
import br.com.archbase.starter.security.auto.configuration.ArchbaseSecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ArchbaseMultitenancyAutoConfiguration.class, ArchbaseCoreAutoConfiguration.class, ArchbaseSecurityAutoConfiguration.class})
public class ArchbaseStarterAutoConfiguration {
}
