package br.com.archbase.starter.auto.configuration;


import br.com.archbase.starter.core.auto.configuration.ArchbaseCoreAutoConfiguration;
import br.com.archbase.starter.multitenantcy.auto.configuration.ArchbaseMultitenancyAutoConfiguration;
import br.com.archbase.starter.security.auto.configuration.ArchbaseSecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ArchbaseCoreAutoConfiguration.class, ArchbaseSecurityAutoConfiguration.class, ArchbaseMultitenancyAutoConfiguration.class})
public class ArchbaseStarterAutoConfiguration {
}
