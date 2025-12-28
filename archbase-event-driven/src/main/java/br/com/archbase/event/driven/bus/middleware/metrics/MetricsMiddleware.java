package br.com.archbase.event.driven.bus.middleware.metrics;

import br.com.archbase.event.driven.spec.command.contracts.Command;
import br.com.archbase.event.driven.spec.message.contracts.Message;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.middleware.contracts.NextMiddlewareFunction;
import br.com.archbase.event.driven.spec.query.contracts.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeUnit;

/**
 * Middleware para coletar métricas de Commands, Queries e Events.
 * <p>
 * Coleta:
 * <ul>
 *   <li>Contador de mensagens processadas</li>
 *   <li>Contador de erros</li>
 *   <li>Timer de tempo de processamento</li>
 * </ul>
 * <p>
 * Requer Micrometer no classpath. Se não disponível, as métricas são desabilitadas automaticamente.
 */
public class MetricsMiddleware implements Middleware {

    private static final Log log = LogFactory.getLog(MetricsMiddleware.class);
    private static final boolean MICROMETER_AVAILABLE = isMicrometerAvailable();

    private final Object meterRegistry;
    private final boolean enabled;

    /**
     * Cria um MetricsMiddleware com Micrometer se disponível.
     */
    public MetricsMiddleware() {
        this.meterRegistry = getMeterRegistry();
        this.enabled = this.meterRegistry != null && MICROMETER_AVAILABLE;
        if (!enabled) {
            log.info("Micrometer não encontrado. MetricsMiddleware desabilitado.");
        }
    }

    /**
     * Cria um MetricsMiddleware com registry específico.
     *
     * @param meterRegistry MeterRegistry do Micrometer (opcional)
     */
    public MetricsMiddleware(Object meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.enabled = meterRegistry != null && MICROMETER_AVAILABLE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R handle(Message<R> message, NextMiddlewareFunction<Message<R>, R> next) {
        if (!enabled) {
            return next.call(message);
        }

        String messageType = getMessageType(message);
        String messageName = message.getClass().getSimpleName();

        long startTime = System.nanoTime();

        try {
            R result = next.call(message);

            long duration = System.nanoTime() - startTime;
            recordSuccess(messageType, messageName, duration);
            return result;

        } catch (Exception ex) {
            long duration = System.nanoTime() - startTime;
            recordError(messageType, messageName, ex, duration);
            throw ex;
        }
    }

    private String getMessageType(Message<?> message) {
        if (message instanceof Command<?>) {
            return "command";
        } else if (message instanceof Query<?>) {
            return "query";
        } else {
            return "event";
        }
    }

    private static boolean isMicrometerAvailable() {
        try {
            Class.forName("io.micrometer.core.instrument.MeterRegistry");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Object getMeterRegistry() {
        if (!MICROMETER_AVAILABLE) {
            return null;
        }
        try {
            // Tenta obter do contexto do Spring se disponível
            Class<?> metricsClass = Class.forName("io.micrometer.core.instrument.Metrics");
            return metricsClass.getMethod("globalRegistry").invoke(null);
        } catch (Exception e) {
            log.warn("Não foi possível obter MeterRegistry global do Micrometer");
            return null;
        }
    }

    private void recordSuccess(String type, String message, long durationNanos) {
        if (!enabled) return;
        try {
            // Incrementar contador de sucesso
            incrementCounter("archbase." + type + ".success", type, message, null);
            incrementCounter("archbase." + type + ".dispatched", type, message, null);

            // Registrar timer
            recordTimer("archbase." + type + ".duration", type, message, "success", durationNanos);
        } catch (Exception e) {
            log.debug("Erro ao registrar métricas: " + e.getMessage());
        }
    }

    private void recordError(String type, String message, Exception ex, long durationNanos) {
        if (!enabled) return;
        try {
            // Incrementar contador de erro
            incrementCounter("archbase." + type + ".error", type, message, ex.getClass().getSimpleName());

            // Registrar timer
            recordTimer("archbase." + type + ".duration", type, message, "error", durationNanos);
        } catch (Exception e) {
            log.debug("Erro ao registrar métricas: " + e.getMessage());
        }
    }

    private void incrementCounter(String name, String type, String message, String exception) {
        try {
            Class<?> counterClass = Class.forName("io.micrometer.core.instrument.Counter");
            Class<?> counterBuilderClass = Class.forName("io.micrometer.core.instrument.Counter$Builder");

            Object builder = counterBuilderClass
                    .getMethod("builder", String.class)
                    .invoke(null, name);

            // Adicionar tags
            builder = counterBuilderClass
                    .getMethod("tag", String.class, String.class)
                    .invoke(builder, "type", type);
            builder = counterBuilderClass
                    .getMethod("tag", String.class, String.class)
                    .invoke(builder, "message", message);

            if (exception != null) {
                builder = counterBuilderClass
                        .getMethod("tag", String.class, String.class)
                        .invoke(builder, "exception", exception);
            }

            Object counter = counterBuilderClass
                    .getMethod("register", Class.forName("io.micrometer.core.instrument.MeterRegistry"))
                    .invoke(builder, meterRegistry);

            counterClass.getMethod("increment").invoke(counter);
        } catch (Exception e) {
            log.trace("Erro ao incrementar contador: " + e.getMessage());
        }
    }

    private void recordTimer(String name, String type, String message, String status, long durationNanos) {
        try {
            Class<?> timerClass = Class.forName("io.micrometer.core.instrument.Timer");
            Class<?> sampleClass = Class.forName("io.micrometer.core.instrument.Timer$Sample");

            Object builder = timerClass
                    .getMethod("builder", String.class)
                    .invoke(null, name);

            // Adicionar tags
            builder = timerClass
                    .getMethod("tag", String.class, String.class)
                    .invoke(builder, "type", type);
            builder = timerClass
                    .getMethod("tag", String.class, String.class)
                    .invoke(builder, "message", message);
            builder = timerClass
                    .getMethod("tag", String.class, String.class)
                    .invoke(builder, "status", status);

            Object timer = timerClass
                    .getMethod("register", Class.forName("io.micrometer.core.instrument.MeterRegistry"))
                    .invoke(builder, meterRegistry);

            // Registrar tempo
            timerClass
                    .getMethod("record", Object.class, long.class, TimeUnit.class)
                    .invoke(timer, null, durationNanos, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            log.trace("Erro ao registrar timer: " + e.getMessage());
        }
    }

    /**
     * Verifica se o middleware está habilitado.
     *
     * @return true se habilitado
     */
    public boolean isEnabled() {
        return enabled;
    }
}
