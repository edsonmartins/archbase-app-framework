package br.com.archbase.event.driven.bus.middleware.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * O uso efetivo do Micrometer. Só é carregada quando o Micrometer está no classpath: o {@link MetricsMiddleware}
 * confere antes de referenciá-la.
 */
final class MetricasMicrometer {

    private final MeterRegistry registry;

    private MetricasMicrometer(MeterRegistry registry) {
        this.registry = registry;
    }

    static MetricasMicrometer global() {
        return new MetricasMicrometer(Metrics.globalRegistry);
    }

    static MetricasMicrometer de(Object registry) {
        if (!(registry instanceof MeterRegistry meterRegistry)) {
            throw new IllegalArgumentException("Esperado io.micrometer.core.instrument.MeterRegistry, recebido "
                    + (registry == null ? "null" : registry.getClass().getName()));
        }
        return new MetricasMicrometer(meterRegistry);
    }

    void sucesso(String tipo, String mensagem, long nanos) {
        Counter.builder("archbase." + tipo + ".success").tag("type", tipo).tag("message", mensagem)
                .register(registry).increment();
        Counter.builder("archbase." + tipo + ".dispatched").tag("type", tipo).tag("message", mensagem)
                .register(registry).increment();
        tempo(tipo, mensagem, "success", nanos);
    }

    void erro(String tipo, String mensagem, String excecao, long nanos) {
        Counter.builder("archbase." + tipo + ".error").tag("type", tipo).tag("message", mensagem)
                .tag("exception", excecao).register(registry).increment();
        tempo(tipo, mensagem, "error", nanos);
    }

    private void tempo(String tipo, String mensagem, String status, long nanos) {
        Timer.builder("archbase." + tipo + ".duration").tag("type", tipo).tag("message", mensagem)
                .tag("status", status).register(registry).record(nanos, TimeUnit.NANOSECONDS);
    }
}
