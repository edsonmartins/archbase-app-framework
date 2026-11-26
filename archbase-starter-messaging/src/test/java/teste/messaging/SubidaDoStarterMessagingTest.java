package teste.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** O starter-messaging num worker, sem web: é o caso para o qual ele existe. */
@DisplayName("archbase-starter-messaging num worker")
class SubidaDoStarterMessagingTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration",
            "org.springframework.boot.activemq.autoconfigure.ActiveMQAutoConfiguration",
            "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"})
    static class Worker {
    }

    @Test
    @DisplayName("sobe sem web, sem banco e sem broker")
    void sobe() {
        SpringApplication app = new SpringApplication(Worker.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext ctx = app.run()) {
            assertThat(ctx.isActive()).isTrue();
        }
    }

    @Test
    @DisplayName("numa aplicação web também sobe, com um único tratador de erros (antes, não subia)")
    void web() {
        SpringApplication app = new SpringApplication(Worker.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        try (ConfigurableApplicationContext ctx = app.run("--server.port=0")) {
            assertThat(ctx.getBeanNamesForType(br.com.archbase.error.handling.ErrorHandlingControllerAdvice.class))
                    .hasSize(1);
        }
    }
}
