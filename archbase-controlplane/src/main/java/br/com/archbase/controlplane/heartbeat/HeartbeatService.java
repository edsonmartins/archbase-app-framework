package br.com.archbase.controlplane.heartbeat;

import br.com.archbase.controlplane.mqtt.ControlPlaneMqttClient;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Heartbeat da instância: versão do produto, tenants atendidos, saúde e consumo metered ainda não reportado.
 * <p>
 * Vai pelo MQTT ({@code cp/v1/inst/{id}/evt}) e, com o broker fora do ar, por HTTPS. As linhas de consumo enviadas só
 * são marcadas como reportadas depois da confirmação — o broker confirmou o recebimento (QoS 1) ou o Control Plane
 * respondeu 2xx. Sem confirmação, vão de novo no próximo heartbeat.
 *
 * @since 3.4.6
 */
public class HeartbeatService implements SmartLifecycle {

    /** Canal pelo qual o heartbeat saiu. */
    public enum Channel { MQTT, HTTPS, NONE }

    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);
    private static final int MAX_USAGE_ROWS = 5000;

    private final ControlPlaneMqttClient mqtt;
    private final ControlPlaneState state;
    private final ControlPlaneStateStore store;
    private final String instanceId;
    private final String product;
    private final String productVersion;
    private final String httpsUrl;
    private final String credential;
    private final Duration interval;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    private volatile ScheduledExecutorService scheduler;
    private volatile boolean running;

    public HeartbeatService(ControlPlaneMqttClient mqtt, ControlPlaneState state, ControlPlaneStateStore store,
                            String instanceId, String product, String productVersion, String httpsUrl,
                            String credential, Duration interval) {
        this.mqtt = mqtt;
        this.state = state;
        this.store = store;
        this.instanceId = instanceId;
        this.product = product;
        this.productVersion = productVersion;
        this.httpsUrl = httpsUrl;
        this.credential = credential;
        this.interval = interval;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "controlplane-heartbeat");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::sendSafely, interval.toMillis(), interval.toMillis(),
                TimeUnit.MILLISECONDS);
        running = true;
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void sendSafely() {
        try {
            send();
        } catch (RuntimeException e) {
            log.warn("controlplane.heartbeat.error erro={}", e.getMessage());
        }
    }

    /** Envia um heartbeat agora. Devolve por onde ele saiu ({@link Channel#NONE} se nenhum canal confirmou). */
    public synchronized Channel send() {
        List<ControlPlaneStateStore.UsageRow> rows = store.unreportedRows(MAX_USAGE_ROWS);
        ObjectNode payload = payload(rows);
        List<String> ids = rows.stream().map(ControlPlaneStateStore.UsageRow::id).toList();

        if (mqtt != null && mqtt.isConnected()) {
            try {
                mqtt.publishEvent(payload).waitForCompletion(10_000);
                store.markReported(ids);
                log.info("controlplane.heartbeat.sent channel=MQTT tenants={} usage_rows={}", state.tenantIds().size(),
                        ids.size());
                return Channel.MQTT;
            } catch (Exception e) {
                log.warn("controlplane.heartbeat.mqtt_failed erro={} — tentando HTTPS", e.getMessage());
            }
        }
        if (httpsUrl != null && !httpsUrl.isBlank()) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(httpsUrl))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + credential)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload)))
                        .build();
                HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() / 100 == 2) {
                    store.markReported(ids);
                    log.info("controlplane.heartbeat.sent channel=HTTPS tenants={} usage_rows={}",
                            state.tenantIds().size(), ids.size());
                    return Channel.HTTPS;
                }
                log.warn("controlplane.heartbeat.https_failed status={}", response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("controlplane.heartbeat.https_failed erro={}", e.getMessage());
            }
        }
        log.error("controlplane.heartbeat.undelivered usage_rows={} — fica para o próximo", ids.size());
        return Channel.NONE;
    }

    ObjectNode payload(List<ControlPlaneStateStore.UsageRow> rows) {
        ObjectNode node = json.createObjectNode();
        node.put("event", "heartbeat");
        node.put("instanceId", instanceId);
        node.put("product", product);
        node.put("version", productVersion);
        node.put("health", "UP");
        node.put("license", state.licenseStatus().name());
        ArrayNode tenants = node.putArray("tenants");
        state.tenantIds().stream().sorted().forEach(tenants::add);
        Map<String, Long> aggregated = new LinkedHashMap<>();
        for (ControlPlaneStateStore.UsageRow row : rows) {
            aggregated.merge(row.tenantId() + "\u0000" + row.key(), row.amount(), Long::sum);
        }
        ArrayNode usage = node.putArray("usage");
        for (Map.Entry<String, Long> entry : aggregated.entrySet()) {
            String[] parts = entry.getKey().split("\u0000", 2);
            ObjectNode item = usage.addObject();
            item.put("tenantId", parts[0]);
            item.put("key", parts[1]);
            item.put("amount", entry.getValue());
        }
        node.put("at", Instant.now().toString());
        return node;
    }
}
