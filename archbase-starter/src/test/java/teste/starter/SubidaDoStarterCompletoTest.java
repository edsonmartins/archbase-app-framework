package teste.starter;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.repository.UserJpaRepository;
import jakarta.servlet.Filter;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O {@code archbase-starter} como o consumidor o recebe: só auto-configuração, sem varrer {@code br.com.archbase}.
 * <p>
 * Os dois primeiros casos são a guarda do "não quebrar o que funciona": o arranjo padrão e o de quem monta tudo à
 * mão (varre os pacotes do framework, declara os repositórios e desliga a configuração MVC do starter-core com um
 * {@code customWebMvcConfigurer}). Os demais fixam o que estava errado.
 */
@DisplayName("Subida do archbase-starter completo")
class SubidaDoStarterCompletoTest {

    @Test
    @DisplayName("padrão: segurança do Archbase ativa, um resolvedor de tenant, rota protegida, tenant no @Async")
    void padrao() throws Exception {
        try (ConfigurableApplicationContext ctx = subir(AppPadrao.class)) {
            assertThat(ctx.getBeansOfType(ArchbaseJwtAuthenticationFilter.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(UserJpaRepository.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(CurrentTenantIdentifierResolver.class)).hasSize(1);
            mockMvc(ctx).perform(get("/api/v1/user/qualquer")).andExpect(status().isUnauthorized());
            String[] visto = vistoNoAsync(ctx).split("\\|");
            assertThat(visto[0]).isEqualTo("tenant-do-chamador");
            assertThat(visto[1]).as("usuário autenticado chega ao @Async").isEqualTo("pessoa-do-chamador");
            assertThat(visto[2]).as("executor do @Async não muda").startsWith("SimpleAsyncTaskExecutor");
        }
    }

    @Test
    @DisplayName("quem varre o framework e declara os repositórios à mão continua subindo, sem bean duplicado")
    void montagemManual() {
        try (ConfigurableApplicationContext ctx = subir(AppMontadaAMao.class)) {
            assertThat(ctx.getBeansOfType(ArchbaseJwtAuthenticationFilter.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(UserJpaRepository.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(CurrentTenantIdentifierResolver.class)).hasSize(1);
        }
    }

    @Test
    @DisplayName("quem varre o framework sem desligar o MVC do starter também sobe, sem repositório duplicado")
    void varreduraPropriaComMvcDoStarter() {
        try (ConfigurableApplicationContext ctx = subir(AppQueVarreMasUsaOMvcDoStarter.class)) {
            assertThat(ctx.getBeansOfType(UserJpaRepository.class)).hasSize(1);
        }
    }

    @Test
    @DisplayName("archbase.security.enabled=false desliga a segurança do Archbase")
    void segurancaDesligada() {
        try (ConfigurableApplicationContext ctx = subir(AppPadrao.class, "--archbase.security.enabled=false")) {
            assertThat(ctx.getBeansOfType(ArchbaseJwtAuthenticationFilter.class)).isEmpty();
            assertThat(ctx.getBeansOfType(UserJpaRepository.class)).isEmpty();
            // O tenant não depende da segurança.
            assertThat(ctx.getBeansOfType(CurrentTenantIdentifierResolver.class)).hasSize(1);
        }
    }

    @Test
    @DisplayName("archbase.web.mvc.enabled=false não leva a segurança junto")
    void mvcDesligadoMantemSeguranca() throws Exception {
        try (ConfigurableApplicationContext ctx = subir(AppPadrao.class, "--archbase.web.mvc.enabled=false")) {
            assertThat(ctx.getBeansOfType(ArchbaseJwtAuthenticationFilter.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(CurrentTenantIdentifierResolver.class)).hasSize(1);
            mockMvc(ctx).perform(get("/api/v1/user/qualquer")).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("aplicação com bean taskExecutor próprio: o @Async continua nele")
    void executorDaAplicacaoContinua() throws Exception {
        try (ConfigurableApplicationContext ctx = subir(AppComExecutorProprio.class)) {
            assertThat(vistoNoAsync(ctx).split("\\|")[2]).startsWith("executor-da-app-");
        }
    }

    @Test
    @DisplayName("segurança desligada também sobe sem o auditorAware da segurança")
    void segurancaDesligadaSobeSemAuditor() {
        try (ConfigurableApplicationContext ctx = subir(AppPadrao.class, "--archbase.security.enabled=false")) {
            assertThat(ctx.getBean("auditorAware")).isNotNull();
        }
    }

    // ── aplicações ───────────────────────────────────────────────────────────────────────────────

    /** Consumidor típico: nenhuma varredura de br.com.archbase. */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class AppPadrao {
        @Bean
        ServicoAssincrono servicoAssincrono() {
            return new ServicoAssincrono();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class AppComExecutorProprio {
        @Bean
        ServicoAssincrono servicoAssincrono() {
            return new ServicoAssincrono();
        }

        @Bean
        ThreadPoolTaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("executor-da-app-");
            return executor;
        }
    }

    static class ServicoAssincrono {
        @Async
        public CompletableFuture<String> tenantAtual() {
            var autenticacao = SecurityContextHolder.getContext().getAuthentication();
            return CompletableFuture.completedFuture(ArchbaseTenantContext.getTenantId() + "|"
                    + (autenticacao == null ? null : autenticacao.getName()) + "|" + Thread.currentThread().getName());
        }
    }

    /** Quem monta à mão: varre o framework, declara entidades e repositórios, e troca o MVC do starter-core. */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"br.com.archbase.security", "br.com.archbase.multitenancy"})
    @EntityScan(basePackages = "br.com.archbase.security.persistence")
    @EnableJpaRepositories(basePackages = "br.com.archbase.security.repository",
            repositoryBaseClass = CommonArchbaseJpaRepository.class)
    static class AppMontadaAMao {
        @Bean
        WebMvcConfigurer customWebMvcConfigurer() {
            return new WebMvcConfigurer() {
            };
        }
    }

    /** Varre e declara os repositórios, mas deixa o MVC do starter-core ligado. */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"br.com.archbase.security", "br.com.archbase.multitenancy"})
    @EnableJpaRepositories(basePackages = "br.com.archbase.security.repository",
            repositoryBaseClass = CommonArchbaseJpaRepository.class)
    static class AppQueVarreMasUsaOMvcDoStarter {
    }

    // ── apoio ────────────────────────────────────────────────────────────────────────────────────

    static ConfigurableApplicationContext subir(Class<?> app, String... extras) {
        List<String> args = new ArrayList<>(List.of(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:starter-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
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
        // Um extra com a mesma chave substitui o padrão (repetida, a propriedade viraria lista).
        for (String extra : extras) {
            String chave = extra.substring(0, extra.indexOf('=') + 1);
            args.removeIf(arg -> arg.startsWith(chave));
        }
        args.addAll(List.of(extras));
        return new SpringApplication(app).run(args.toArray(String[]::new));
    }

    static String vistoNoAsync(ConfigurableApplicationContext ctx) throws Exception {
        ArchbaseTenantContext.setTenantId("tenant-do-chamador");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("pessoa-do-chamador", null, List.of()));
        try {
            return ctx.getBean(ServicoAssincrono.class).tenantAtual().get(10, TimeUnit.SECONDS);
        } finally {
            ArchbaseTenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    static MockMvc mockMvc(ConfigurableApplicationContext ctx) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx)
                .addFilters(ctx.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }
}
