package br.com.archbase.controlplane.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

/**
 * Métricas do Control Plane. Cada caso da tabela "Comportamento em falha" tem um contador ou gauge, para o alerta
 * existir no Grafana — as regras prontas estão em {@code alerts/controlplane-alerts.yml}.
 * <p>
 * O módulo só registra no {@link MeterRegistry} da aplicação; quem exporta é ela (com
 * {@code micrometer-registry-prometheus}, o VictoriaMetrics coleta em {@code /actuator/prometheus}). Sem registry, as
 * métricas não vão a lugar nenhum e nada quebra.
 * <p>
 * Nenhuma métrica tem o tenant como tag: o número de tenants não tem teto e o tenant já está no log estruturado de
 * cada caso.
 *
 * @since 3.4.6
 */
public class ControlPlaneMetrics {

    /** Para código montado à mão (testes, instâncias extras): registra num registry vazio, que descarta tudo. */
    public static final ControlPlaneMetrics NOOP = new ControlPlaneMetrics(new CompositeMeterRegistry());

    public static final String TOKEN_REJECTED = "controlplane.token.rejected";
    public static final String ACCESS_DENIED = "controlplane.access.denied";
    public static final String RESTRICTED_DENIED = "controlplane.restricted.denied";
    public static final String COMMANDS = "controlplane.commands";
    public static final String HEARTBEAT = "controlplane.heartbeat";
    public static final String USAGE_EXCEEDED = "controlplane.usage.exceeded";

    private final MeterRegistry registry;

    public ControlPlaneMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public MeterRegistry registry() {
        return registry;
    }

    /** Token do Control Plane recusado, por código ({@code WRONG_AUDIENCE}, {@code TENANT_MISMATCH}...). */
    public void tokenRejected(String code) {
        counter(TOKEN_REJECTED, "Tokens do Control Plane recusados", "code", code).increment();
    }

    /** {@code @RequiresAdmin}, {@code @RequiresSeat} ou {@code @RequiresEntitlement} recusou. */
    public void accessDenied(String code) {
        counter(ACCESS_DENIED, "Acessos recusados por regra do Control Plane", "code", code).increment();
    }

    /** Escrita no modo restrito ({@code LICENSE_EXPIRED}, {@code TENANT_SUSPENDED}); {@code report} também conta. */
    public void restrictedDenied(String code, String enforcement) {
        Counter.builder(RESTRICTED_DENIED)
                .description("Escritas barradas (ou que seriam, com enforcement=report) pelo modo restrito")
                .tag("code", tag(code))
                .tag("enforcement", tag(enforcement))
                .register(registry)
                .increment();
    }

    /** Comando processado: {@code applied}, {@code duplicate}, {@code failed} ou {@code rejected}. */
    public void command(String type, String status) {
        Counter.builder(COMMANDS)
                .description("Comandos do Control Plane processados, por tipo e resultado")
                .tag("type", tag(type))
                .tag("status", tag(status))
                .register(registry)
                .increment();
    }

    /** Heartbeat enviado por {@code mqtt} ou {@code https}, ou não entregue ({@code none}). */
    public void heartbeat(String channel) {
        counter(HEARTBEAT, "Heartbeats por canal; none = não entregue", "channel", channel).increment();
    }

    /** Consumo metered passou do limite (tolerado). */
    public void usageExceeded(String key) {
        counter(USAGE_EXCEEDED, "Consumos metered acima do limite", "key", key).increment();
    }

    private Counter counter(String name, String description, String tagKey, String tagValue) {
        return Counter.builder(name).description(description).tag(tagKey, tag(tagValue)).register(registry);
    }

    private static String tag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
