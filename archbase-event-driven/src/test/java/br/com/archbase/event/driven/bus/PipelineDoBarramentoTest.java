package br.com.archbase.event.driven.bus;

import br.com.archbase.event.driven.bus.handlers.Somar;
import br.com.archbase.event.driven.bus.middleware.metrics.MetricsMiddleware;
import br.com.archbase.event.driven.bus.spring.MessageBusSpringConfiguration;
import br.com.archbase.event.driven.spec.annotations.HandlerScan;
import br.com.archbase.event.driven.spec.command.contracts.CommandBus;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.spring.contracts.MiddlewareConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Barramento de comandos: pipeline e métricas")
class PipelineDoBarramentoTest {

    static final List<String> PASSOU = new CopyOnWriteArrayList<>();
    static final SimpleMeterRegistry REGISTRY = new SimpleMeterRegistry();

    static Middleware marcador(String nome) {
        return new Middleware() {
            @Override
            public <R> R handle(br.com.archbase.event.driven.spec.message.contracts.Message<R> message,
                                br.com.archbase.event.driven.spec.middleware.contracts.NextMiddlewareFunction<
                                        br.com.archbase.event.driven.spec.message.contracts.Message<R>, R> next) {
                PASSOU.add(nome);
                return next.call(message);
            }
        };
    }

    @Configuration
    @Import(MessageBusSpringConfiguration.class)
    @HandlerScan(basePackages = "br.com.archbase.event.driven.bus.handlers")
    static class Config implements MiddlewareConfig {
        @Override
        public List<Middleware> getCommandMiddlewarePipeline() {
            return List.of(marcador("comando"), new MetricsMiddleware(REGISTRY));
        }

        @Override
        public List<Middleware> getQueryMiddlewarePipeline() {
            return List.of(marcador("consulta"));
        }
    }

    @Test
    @DisplayName("o CommandBus usa o pipeline de comandos, e o MetricsMiddleware registra sucesso e erro")
    void comando() {
        try (var ctx = new AnnotationConfigApplicationContext(Config.class)) {
            CommandBus bus = ctx.getBean(CommandBus.class);

            assertThat(bus.dispatch(new Somar(2, 3))).isEqualTo(5);
            // Até a 3.5.0 o CommandBus recebia o pipeline de consultas.
            assertThat(PASSOU).containsExactly("comando");

            assertThatThrownBy(() -> bus.dispatch(new Somar(-1, 3))).isInstanceOf(IllegalArgumentException.class);

            // Até a 3.5.0 nenhuma métrica era registrada (reflexão com nomes errados, erros engolidos).
            assertThat(REGISTRY.get("archbase.command.success").tag("message", "Somar").counter().count()).isEqualTo(1.0);
            assertThat(REGISTRY.get("archbase.command.error").tag("exception", "IllegalArgumentException").counter().count())
                    .isEqualTo(1.0);
            assertThat(REGISTRY.get("archbase.command.duration").tag("status", "success").timer().count()).isEqualTo(1);
        }
    }
}
