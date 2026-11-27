package br.com.archbase.starter.hypersistence.auto.configuration;

import br.com.archbase.hypersistence.config.ArchbaseHypersistenceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Até a 3.5.0 o starter registrava dois beans ArchbaseHypersistenceProperties, e injetá-lo sem qualificador falhava. */
@DisplayName("archbase-starter-hypersistence")
class SubidaDoHypersistenceTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
    }

    @Test
    @DisplayName("um único bean de propriedades, injetável por tipo")
    void umBeanDePropriedades() {
        try (ConfigurableApplicationContext ctx = new SpringApplication(App.class).run(
                "--spring.main.web-application-type=none",
                "--spring.datasource.url=jdbc:h2:mem:hypersistence-starter;DB_CLOSE_DELAY=-1")) {
            assertThat(ctx.getBeanNamesForType(ArchbaseHypersistenceProperties.class)).hasSize(1);
            assertThat(ctx.getBean(ArchbaseHypersistenceProperties.class)).isNotNull();
        }
    }
}
