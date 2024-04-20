package br.com.archbase.starter.core.auto.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(value = {
        ArchbaseServerMvcConfiguration.class,
        ArchbaseThreadConfiguration.class, ArchbaseRSQLConfiguration.class,
        ArchbaseBeanValidateConfiguration.class,
//        ArchbaseSwaggerConfiguration.class
})
public class ArchbaseCoreAutoConfiguration {
}
