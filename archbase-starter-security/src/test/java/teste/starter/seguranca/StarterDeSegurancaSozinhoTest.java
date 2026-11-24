package teste.starter.seguranca;

import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.repository.UserJpaRepository;
import jakarta.servlet.Filter;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O {@code archbase-starter-security} sem o {@code archbase-starter-core}: até a 3.5.0 não subia, porque os componentes
 * de segurança só eram registrados pela varredura do core. Este módulo não depende do core, então o classpath do
 * teste é exatamente o de quem declara só este starter.
 */
@DisplayName("archbase-starter-security sozinho")
class StarterDeSegurancaSozinhoTest {

    @Test
    @DisplayName("sobe com filtro JWT, repositórios e resolvedor de tenant; rota protegida exige autenticação")
    void sobe() throws Exception {
        try (ConfigurableApplicationContext ctx = subir()) {
            assertThat(ctx.getBeansOfType(ArchbaseJwtAuthenticationFilter.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(UserJpaRepository.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(CurrentTenantIdentifierResolver.class)).hasSize(1);
            MockMvc mvc = mockMvc(ctx);
            mvc.perform(get("/api/v1/user/qualquer")).andExpect(status().isUnauthorized());
            // O login vai ao banco (consulta de usuário com @TenantId): 401 de credencial, não 500 de contexto.
            mvc.perform(post("/api/v1/auth/authenticate")
                            .header("X-TENANT-ID", "tenant-a")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ninguem@x.com\",\"password\":\"errada\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("archbase.security.enabled=false não registra os componentes de segurança")
    void desligado() {
        try (ConfigurableApplicationContext ctx = subir("--archbase.security.enabled=false")) {
            assertThat(ctx.getBeansOfType(ArchbaseJwtAuthenticationFilter.class)).isEmpty();
            assertThat(ctx.getBeansOfType(UserJpaRepository.class)).isEmpty();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
    }

    static ConfigurableApplicationContext subir(String... extras) {
        List<String> args = new ArrayList<>(List.of(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:seg-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--archbase.security.schema.mode=off",
                "--archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "--archbase.security.jwt.token-expiration=3600000",
                "--archbase.security.jwt.refresh-expiration=86400000",
                "--archbase.security.whitelist=",
                "--archbase.security.cors.allowed-origins=*",
                "--archbase.security.cors.allowed-methods=*",
                "--archbase.security.cors.allowed-headers=*",
                "--archbase.security.cors.allow-credentials=false"));
        args.addAll(List.of(extras));
        return new SpringApplication(App.class).run(args.toArray(String[]::new));
    }

    static MockMvc mockMvc(ConfigurableApplicationContext ctx) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx)
                .addFilters(ctx.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }
}
