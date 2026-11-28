package br.com.archbase.ddd.infraestructure.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AsynchronousEventHandler")
class EventoAssincronoTest {

    public static class Ouvinte {
        final CompletableFuture<String> thread = new CompletableFuture<>();

        public void ao(String evento) {
            thread.complete(Thread.currentThread().getName());
        }
    }

    @Test
    @DisplayName("roda fora da thread de quem publicou (antes, na mesma)")
    void foraDaThread() throws Exception {
        DefaultListableBeanFactory fabrica = new DefaultListableBeanFactory();
        Ouvinte ouvinte = new Ouvinte();
        fabrica.registerSingleton("ouvinte", ouvinte);
        var handler = new AsynchronousEventHandler(String.class, "ouvinte",
                Ouvinte.class.getMethod("ao", String.class), fabrica);

        handler.handle("pedido-criado");

        assertThat(ouvinte.thread.get(5, TimeUnit.SECONDS)).isNotEqualTo(Thread.currentThread().getName());
    }
}
