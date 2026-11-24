package br.com.archbase.starter.core.auto.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O executor do starter-core aplica o {@link TaskDecorator} do contexto — com o starter de multitenancy, é o que leva
 * o tenant para a tarefa. Até a 3.5.0 o decorator era ignorado.
 */
@DisplayName("Executor do starter-core aplica o TaskDecorator")
class ExecutorAplicaDecoratorTest {

    private static final ThreadLocal<String> MARCA = new ThreadLocal<>();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ArchbaseThreadConfiguration.class);

    @Test
    @DisplayName("com um TaskDecorator no contexto, a tarefa roda decorada")
    void aplica() {
        runner.withBean(TaskDecorator.class, () -> tarefa -> {
                    String valor = MARCA.get();
                    return () -> {
                        MARCA.set(valor);
                        try {
                            tarefa.run();
                        } finally {
                            MARCA.remove();
                        }
                    };
                })
                .run(ctx -> assertThat(executar(ctx.getBean(TaskExecutor.class))).isEqualTo("do-chamador"));
    }

    @Test
    @DisplayName("sem TaskDecorator, o executor sobe igual a antes")
    void semDecorator() {
        runner.run(ctx -> assertThat(executar(ctx.getBean(TaskExecutor.class))).isNull());
    }

    private static String executar(TaskExecutor executor) throws Exception {
        MARCA.set("do-chamador");
        try {
            CompletableFuture<String> visto = new CompletableFuture<>();
            executor.execute(() -> visto.complete(MARCA.get()));
            return visto.get(10, TimeUnit.SECONDS);
        } finally {
            MARCA.remove();
        }
    }
}
