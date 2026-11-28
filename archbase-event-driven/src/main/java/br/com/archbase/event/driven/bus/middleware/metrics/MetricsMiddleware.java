package br.com.archbase.event.driven.bus.middleware.metrics;

import br.com.archbase.event.driven.spec.command.contracts.Command;
import br.com.archbase.event.driven.spec.message.contracts.Message;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.middleware.contracts.NextMiddlewareFunction;
import br.com.archbase.event.driven.spec.query.contracts.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


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

    /** {@link MetricasMicrometer}, guardado como {@code Object} para esta classe carregar sem o Micrometer. */
    private final Object metricas;
    private final boolean enabled;

    /**
     * Usa o {@code Metrics.globalRegistry} do Micrometer, se ele estiver no classpath (o Spring Boot registra o seu
     * registry nele por padrão).
     *
     * <p>Até a 3.5.0 as chamadas ao Micrometer eram feitas por reflexão com nomes errados ({@code globalRegistry} é
     * campo, não método; {@code builder} e {@code tag} procurados na classe errada), os erros eram engolidos e nenhuma
     * métrica era registrada — e cada barramento criado registrava um WARN.
     */
    public MetricsMiddleware() {
        this.metricas = MICROMETER_AVAILABLE ? MetricasMicrometer.global() : null;
        this.enabled = metricas != null;
        if (!enabled) {
            log.debug("Micrometer não encontrado. MetricsMiddleware desabilitado.");
        }
    }

    /**
     * Cria um MetricsMiddleware com registry específico.
     *
     * @param meterRegistry {@code io.micrometer.core.instrument.MeterRegistry}; {@code null} desabilita
     */
    public MetricsMiddleware(Object meterRegistry) {
        this.metricas = MICROMETER_AVAILABLE && meterRegistry != null ? MetricasMicrometer.de(meterRegistry) : null;
        this.enabled = metricas != null;
    }

    @Override
    public <R> R handle(Message<R> message, NextMiddlewareFunction<Message<R>, R> next) {
        if (!enabled) {
            return next.call(message);
        }

        String messageType = getMessageType(message);
        String messageName = message.getClass().getSimpleName();

        long startTime = System.nanoTime();

        try {
            R result = next.call(message);
            registrar(() -> ((MetricasMicrometer) metricas).sucesso(messageType, messageName, System.nanoTime() - startTime));
            return result;
        } catch (RuntimeException ex) {
            long duracao = System.nanoTime() - startTime;
            registrar(() -> ((MetricasMicrometer) metricas).erro(messageType, messageName,
                    ex.getClass().getSimpleName(), duracao));
            throw ex;
        }
    }

    /** Falha de métrica não derruba o despacho. */
    private static void registrar(Runnable registro) {
        try {
            registro.run();
        } catch (RuntimeException e) {
            log.debug("Erro ao registrar métricas: " + e.getMessage());
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
            Class.forName("io.micrometer.core.instrument.MeterRegistry", false, MetricsMiddleware.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
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
