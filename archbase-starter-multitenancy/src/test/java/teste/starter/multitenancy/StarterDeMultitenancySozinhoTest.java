package teste.starter.multitenancy;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O {@code archbase-starter-multitenancy} sem o {@code archbase-starter-core}. Até a 3.5.0 ele punha o tenant no
 * contexto, mas o resolvedor do Hibernate só era registrado pela varredura do core: gravar uma entidade com
 * {@code @TenantId} falhava por falta de quem dissesse o tenant.
 */
@DisplayName("archbase-starter-multitenancy sozinho")
class StarterDeMultitenancySozinhoTest {

    @Test
    @DisplayName("o Hibernate grava e filtra pelo tenant do contexto")
    void isolaPorTenant() {
        try (ConfigurableApplicationContext ctx = subir()) {
            assertThat(ctx.getBeansOfType(CurrentTenantIdentifierResolver.class)).hasSize(1);
            EntityManager em = ctx.getBean(EntityManager.class);
            TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);

            ArchbaseTenantContext.setTenantId("tenant-a");
            Nota nota = new Nota();
            nota.texto = "do tenant a";
            tx.executeWithoutResult(s -> em.persist(nota));
            assertThat(nota.getTenantId()).isEqualTo("tenant-a");

            assertThat(contarNotas(em, tx)).isEqualTo(1);
            ArchbaseTenantContext.setTenantId("tenant-b");
            assertThat(contarNotas(em, tx)).as("outro tenant não enxerga").isZero();
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    @Test
    @DisplayName("tenant e usuário autenticado chegam ao @Async")
    void async() throws Exception {
        try (ConfigurableApplicationContext ctx = subir()) {
            ArchbaseTenantContext.setTenantId("tenant-do-chamador");
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("pessoa-do-chamador", null, List.of()));
            String visto = ctx.getBean(ServicoAssincrono.class).visto().get(10, TimeUnit.SECONDS);
            assertThat(visto).isEqualTo("tenant-do-chamador|pessoa-do-chamador");
        } finally {
            ArchbaseTenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private static long contarNotas(EntityManager em, TransactionTemplate tx) {
        return tx.execute(s -> em.createQuery("select count(n) from Nota n", Long.class).getSingleResult());
    }

    @Entity(name = "Nota")
    @Table(name = "NOTA_TESTE")
    public static class Nota extends TenantPersistenceEntityBase {
        @Column(name = "TEXTO")
        String texto;
    }

    static class ServicoAssincrono {
        @Async
        public CompletableFuture<String> visto() {
            var autenticacao = SecurityContextHolder.getContext().getAuthentication();
            return CompletableFuture.completedFuture(ArchbaseTenantContext.getTenantId() + "|"
                    + (autenticacao == null ? null : autenticacao.getName()));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        @Bean
        ServicoAssincrono servicoAssincrono() {
            return new ServicoAssincrono();
        }
    }

    static ConfigurableApplicationContext subir() {
        SpringApplication app = new SpringApplication(App.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        return app.run(
                "--spring.datasource.url=jdbc:h2:mem:mt-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "--spring.jpa.hibernate.ddl-auto=create-drop");
    }
}
