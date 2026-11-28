package teste.starter;

import br.com.archbase.starter.core.auto.configuration.ArchbaseInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Até a 3.5.0 nenhum bean com @ArchbaseInterceptor era registrado: o registrador ficava fora da varredura. */
@DisplayName("@ArchbaseInterceptor")
class InterceptorAnotadoTest {

    @ArchbaseInterceptor(pathPatterns = "/teste/**")
    public static class Marcador implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            response.setHeader("X-Interceptado", "sim");
            return true;
        }
    }

    @RestController
    public static class Rota {
        @GetMapping("/teste/ping")
        public String ping() {
            return "pong";
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        @Bean
        Marcador marcador() {
            return new Marcador();
        }

        @Bean
        Rota rota() {
            return new Rota();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class AppComConfigurerProprio extends App {
        @Bean
        WebMvcConfigurer customWebMvcConfigurer() {
            return new WebMvcConfigurer() {
            };
        }
    }

    @Test
    @DisplayName("é aplicado com a configuração MVC do starter e com um customWebMvcConfigurer da aplicação")
    void aplicado() throws Exception {
        for (Class<?> app : new Class<?>[]{App.class, AppComConfigurerProprio.class}) {
            try (ConfigurableApplicationContext ctx = SubidaDoStarterCompletoTest.subir(app,
                    "--archbase.security.whitelist=/teste/**")) {
                SubidaDoStarterCompletoTest.mockMvc(ctx).perform(get("/teste/ping"))
                        .andExpect(status().isOk())
                        .andExpect(header().string("X-Interceptado", "sim"));
            }
        }
    }
}
