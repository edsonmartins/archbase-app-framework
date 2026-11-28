package teste.starter.analytics;

import br.com.archbase.analytics.port.AnalyticsAuditPort;
import br.com.archbase.analytics.port.DataScopeProvider;
import br.com.archbase.analytics.port.SavedQueryStorePort;
import br.com.archbase.analytics.proxy.AnalyticsProxyController;
import br.com.archbase.analytics.savedquery.SavedQueryController;
import br.com.archbase.analytics.token.CubeTokenMinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O {@code archbase-starter-analytics} ligado, numa aplicação Spring Boot 4 comum.
 *
 * <p>Até a 3.5.0 o módulo pedia o {@code ObjectMapper} do Jackson 2, que o Boot 4 não registra: o contexto não
 * subia. E as consultas salvas recebiam e devolviam {@code JsonNode} do Jackson 2, que o conversor HTTP do Boot 4
 * não lê nem escreve.
 */
@DisplayName("archbase-starter-analytics ligado")
class SubidaDoAnalyticsTest {

    private static final String SEGREDO = "um-segredo-de-teste-com-mais-de-32-bytes-de-tamanho";

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sobe com os três beans, e o token de escopo é assinado")
    void sobe() {
        try (ConfigurableApplicationContext ctx = subir()) {
            assertThat(ctx.getBeansOfType(AnalyticsProxyController.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(SavedQueryController.class)).hasSize(1);
            String token = ctx.getBean(CubeTokenMinter.class).mint("alice", Map.of("empresa", "e1"), Instant.now());
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    @Test
    @DisplayName("consulta salva: o corpo JSON chega ao port e volta como JSON, não como bean")
    void consultaSalvaIdaEVolta() throws Exception {
        try (ConfigurableApplicationContext ctx = subir()) {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx).build();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "alice", "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")));

            mvc.perform(post("/api/analytics/saved-queries").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"id":"c1","schemaVersion":2,
                                     "query":{"measures":["Pedidos.total"]},
                                     "viz":{"tipo":"barra"},
                                     "meta":{"name":"Pedidos por mês","scope":"private"}}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("c1"))
                    .andExpect(jsonPath("$.meta.name").value("Pedidos por mês"))
                    .andExpect(jsonPath("$.query.measures[0]").value("Pedidos.total"));

            Store store = ctx.getBean(Store.class);
            assertThat(store.salvas.get("c1").queryJson()).isEqualTo("{\"measures\":[\"Pedidos.total\"]}");

            mvc.perform(get("/api/analytics/saved-queries/c1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viz.tipo").value("barra"))
                    .andExpect(jsonPath("$.schemaVersion").value(2));
        }
    }

    // ── aplicação ────────────────────────────────────────────────────────────────────────────────

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        @Bean
        DataScopeProvider dataScopeProvider() {
            return usuario -> Map.of("empresa", "e1");
        }

        @Bean
        AnalyticsAuditPort analyticsAuditPort() {
            return consulta -> { };
        }

        @Bean
        Store store() {
            return new Store();
        }
    }

    static class Store implements SavedQueryStorePort {
        final Map<String, SavedQuery> salvas = new ConcurrentHashMap<>();

        @Override
        public List<SavedQuery> listVisible(String ownerId, String scope) {
            return salvas.values().stream().filter(q -> q.ownerId().equals(ownerId)).toList();
        }

        @Override
        public Optional<SavedQuery> find(String id) {
            return Optional.ofNullable(salvas.get(id));
        }

        @Override
        public SavedQuery save(String id, String name, String ownerId, String scope, int schemaVersion,
                               String queryJson, String vizJson) {
            SavedQuery q = new SavedQuery(id, name, ownerId, scope, schemaVersion, queryJson, vizJson,
                    Instant.EPOCH, Instant.EPOCH);
            salvas.put(id, q);
            return q;
        }

        @Override
        public boolean remove(String id, String ownerId) {
            return salvas.remove(id) != null;
        }
    }

    private static ConfigurableApplicationContext subir() {
        return new SpringApplication(App.class).run(
                "--server.port=0",
                "--archbase.analytics.enabled=true",
                "--archbase.analytics.secret=" + SEGREDO);
    }
}
