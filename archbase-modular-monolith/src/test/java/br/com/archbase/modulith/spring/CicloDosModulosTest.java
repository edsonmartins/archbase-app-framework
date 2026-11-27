package br.com.archbase.modulith.spring;

import br.com.archbase.modulith.annotations.Module;
import br.com.archbase.modulith.communication.IntegrationEventBus;
import br.com.archbase.modulith.communication.contracts.IntegrationEvent;
import br.com.archbase.modulith.core.ModuleContext;
import br.com.archbase.modulith.core.ModuleLifecycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("Modular monolith: ciclo dos módulos, saúde e eventos por origem")
class CicloDosModulosTest {

    static final List<String> PARADOS = new CopyOnWriteArrayList<>();

    @Module(name = "pedidos")
    public static class Pedidos implements ModuleLifecycle {
        @Override
        public void onStart(ModuleContext context) {
        }

        @Override
        public void onStop() {
            PARADOS.add("pedidos");
        }
    }

    @Module(name = "legado", enabled = false)
    public static class Legado {
    }

    public record PedidoCriado(String id) implements IntegrationEvent {
        @Override
        public String getAggregateId() {
            return id;
        }

        @Override
        public String getAggregateType() {
            return "Pedido";
        }

        @Override
        public String getSourceModule() {
            return "pedidos";
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        @Bean
        Pedidos pedidos() {
            return new Pedidos();
        }

        @Bean
        Legado legado() {
            return new Legado();
        }
    }

    private static ConfigurableApplicationContext subir() {
        return new SpringApplication(App.class).run(
                "--spring.main.web-application-type=none",
                "--spring.datasource.url=jdbc:h2:mem:modulith-ciclo;DB_CLOSE_DELAY=-1");
    }

    @Test
    @DisplayName("módulo desligado não derruba a saúde, e o indicador é registrado pela auto-configuração")
    void saude() {
        try (ConfigurableApplicationContext ctx = subir()) {
            ModuleHealthIndicator indicador = ctx.getBean(ModuleHealthIndicator.class);
            assertThat(indicador.health().getStatus()).isEqualTo(Status.UP);
        }
    }

    @Test
    @DisplayName("fechar o contexto chama onStop dos módulos iniciados")
    void onStop() {
        PARADOS.clear();
        subir().close();
        assertThat(PARADOS).containsExactly("pedidos");
    }

    @Test
    @DisplayName("assinatura por módulo de origem recebe os eventos desse módulo (antes, nenhum)")
    void origem() {
        try (ConfigurableApplicationContext ctx = subir()) {
            IntegrationEventBus barramento = ctx.getBean(IntegrationEventBus.class);
            List<String> dePedidos = new CopyOnWriteArrayList<>();
            List<String> deEstoque = new CopyOnWriteArrayList<>();
            barramento.subscribe(PedidoCriado.class, "pedidos", e -> dePedidos.add(e.id()));
            barramento.subscribe(PedidoCriado.class, "estoque", e -> deEstoque.add(e.id()));

            barramento.publish(new PedidoCriado("p1"));

            await().atMost(5, TimeUnit.SECONDS).until(() -> dePedidos.contains("p1"));
            assertThat(deEstoque).isEmpty();
        }
    }
}
