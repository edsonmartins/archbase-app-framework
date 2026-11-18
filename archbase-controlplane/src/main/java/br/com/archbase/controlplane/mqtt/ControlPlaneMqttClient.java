package br.com.archbase.controlplane.mqtt;

import br.com.archbase.controlplane.command.CommandOutcome;
import br.com.archbase.controlplane.command.CommandProcessor;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Conexão de saída da instância com o broker do Control Plane.
 * <ul>
 * <li>Sessão persistente ({@code cleanStart=false}) e QoS 1: comando publicado com a instância fora chega quando ela
 * volta.</li>
 * <li>Status retido em {@code cp/v1/inst/{id}/status}: {@code online} ao conectar, {@code offline} ao parar, e o
 * mesmo {@code offline} como Last Will — se a instância cair sem se despedir, o broker publica por ela.</li>
 * <li>Comando em {@code cp/v1/inst/{id}/cmd} vai para o {@link CommandProcessor}; o resultado sai em
 * {@code cp/v1/inst/{id}/evt}. O ack ao broker só é enviado depois do processamento: se a instância cair no meio,
 * o broker reentrega, e a idempotência descarta o que já tinha sido aplicado.</li>
 * <li>Broker fora do ar nunca impede a subida: a primeira conexão é tentada em segundo plano, com espera crescente,
 * e depois o Paho reconecta sozinho.</li>
 * </ul>
 *
 * @since 3.4.6
 */
public class ControlPlaneMqttClient implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneMqttClient.class);
    private static final int QOS = 1;

    private final String uri;
    private final String clientId;
    private final String username;
    private final String password;
    private final String instanceId;
    private final String product;
    private final String productVersion;
    private final Duration sessionExpiry;
    private final Duration reconnectMinDelay;
    private final Duration reconnectMaxDelay;
    private final Duration keepAlive;
    private final ControlPlaneTopics topics;
    private final CommandProcessor processor;
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<Consumer<Boolean>> connectListeners = new CopyOnWriteArrayList<>();

    private volatile MqttAsyncClient client;
    private volatile ScheduledExecutorService scheduler;
    private volatile boolean running;
    private volatile Duration nextDelay;

    public ControlPlaneMqttClient(String uri, String clientId, String username, String password, String instanceId,
                                  String product, String productVersion, Duration sessionExpiry,
                                  Duration reconnectMinDelay, Duration reconnectMaxDelay, Duration keepAlive,
                                  CommandProcessor processor) {
        this.uri = uri;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.instanceId = instanceId;
        this.product = product;
        this.productVersion = productVersion;
        this.sessionExpiry = sessionExpiry;
        this.reconnectMinDelay = reconnectMinDelay;
        this.reconnectMaxDelay = reconnectMaxDelay;
        this.keepAlive = keepAlive;
        this.topics = ControlPlaneTopics.forInstance(instanceId);
        this.processor = processor;
    }

    /** Chamado a cada conexão (inclusive reconexão), com {@code true} quando é reconexão. */
    public void onConnect(Consumer<Boolean> listener) {
        connectListeners.add(listener);
    }

    public boolean isConnected() {
        MqttAsyncClient current = client;
        return current != null && current.isConnected();
    }

    public ControlPlaneTopics topics() {
        return topics;
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        try {
            client = new MqttAsyncClient(uri, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            log.error("controlplane.mqtt.client_error uri={} erro={} — canal MQTT desligado", uri, e.getMessage());
            return;
        }
        client.setManualAcks(true);
        client.setCallback(new Callback());
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "controlplane-mqtt");
            t.setDaemon(true);
            return t;
        });
        running = true;
        nextDelay = reconnectMinDelay;
        scheduler.execute(this::connect);
    }

    @Override
    public synchronized void stop() {
        running = false;
        MqttAsyncClient current = client;
        if (current != null) {
            try {
                if (current.isConnected()) {
                    publish(topics.status(), status("offline"), true).waitForCompletion(2000);
                    current.disconnect().waitForCompletion(2000);
                }
            } catch (MqttException e) {
                log.debug("controlplane.mqtt.disconnect_error erro={}", e.getMessage());
            }
            closeQuietly(current);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        log.info("controlplane.mqtt.stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Depois do resto da aplicação: a instância só se anuncia online quando já atende. */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    /**
     * Derruba a conexão sem enviar DISCONNECT, como numa queda da instância: o broker publica o Last Will. Para testes
     * e diagnóstico.
     */
    public synchronized void abortWithoutDisconnect() {
        running = false;
        MqttAsyncClient current = client;
        if (current != null) {
            try {
                current.disconnectForcibly(0, 0, false);
            } catch (MqttException e) {
                log.debug("controlplane.mqtt.abort_error erro={}", e.getMessage());
            }
            closeQuietly(current);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void connect() {
        if (!running) {
            return;
        }
        try {
            client.connect(options()).waitForCompletion(Math.max(5000, keepAlive.toMillis()));
            nextDelay = reconnectMinDelay;
        } catch (MqttException e) {
            Duration delay = nextDelay;
            nextDelay = min(delay.multipliedBy(2), reconnectMaxDelay);
            log.warn("controlplane.mqtt.connect_failed uri={} erro={} — nova tentativa em {}s", uri, e.getMessage(),
                    delay.toSeconds());
            if (running) {
                scheduler.schedule(this::connect, delay.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private MqttConnectionOptions options() {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(false);
        options.setSessionExpiryInterval(sessionExpiry.toSeconds());
        options.setAutomaticReconnect(true);
        options.setAutomaticReconnectDelay((int) Math.max(1, reconnectMinDelay.toSeconds()),
                (int) Math.max(1, reconnectMaxDelay.toSeconds()));
        options.setKeepAliveInterval((int) keepAlive.toSeconds());
        options.setConnectionTimeout(10);
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
        }
        if (password != null && !password.isBlank()) {
            options.setPassword(password.getBytes(StandardCharsets.UTF_8));
        }
        MqttMessage will = new MqttMessage(status("offline"));
        will.setQos(QOS);
        will.setRetained(true);
        options.setWill(topics.status(), will);
        return options;
    }

    // ------------------------------------------------------------------ publicação

    /** Publica um evento (QoS 1) em {@code cp/v1/inst/{id}/evt}. */
    public IMqttToken publishEvent(ObjectNode event) throws MqttException {
        return publish(topics.events(), json.writeValueAsBytes(event), false);
    }

    private IMqttToken publish(String topic, byte[] payload, boolean retained) throws MqttException {
        MqttMessage message = new MqttMessage(payload);
        message.setQos(QOS);
        message.setRetained(retained);
        return client.publish(topic, message);
    }

    private byte[] status(String state) {
        ObjectNode node = json.createObjectNode();
        node.put("state", state);
        node.put("instanceId", instanceId);
        node.put("product", product);
        node.put("version", productVersion);
        node.put("at", Instant.now().toString());
        return json.writeValueAsBytes(node);
    }

    ObjectNode outcomeEvent(CommandOutcome outcome) {
        ObjectNode event = json.createObjectNode();
        event.put("event", "command.result");
        event.put("instanceId", instanceId);
        event.put("commandId", outcome.commandId());
        event.put("command", outcome.type());
        event.put("status", outcome.wireStatus());
        if (outcome.status() == CommandOutcome.Status.DUPLICATE) {
            event.put("duplicate", true);
        }
        if (outcome.reason() != null) {
            event.put("reason", outcome.reason());
        }
        if (!outcome.details().isEmpty()) {
            event.set("details", json.valueToTree(outcome.details()));
        }
        event.put("at", Instant.now().toString());
        return event;
    }

    // ------------------------------------------------------------------ callback

    private final class Callback implements MqttCallback {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            log.info("controlplane.mqtt.connected uri={} reconnect={}", serverURI, reconnect);
            try {
                publish(topics.status(), status("online"), true);
                client.subscribe(topics.commands(), QOS);
            } catch (MqttException e) {
                log.warn("controlplane.mqtt.subscribe_failed erro={}", e.getMessage());
            }
            for (Consumer<Boolean> listener : connectListeners) {
                scheduler.execute(() -> {
                    try {
                        listener.accept(reconnect);
                    } catch (RuntimeException e) {
                        log.warn("controlplane.mqtt.listener_error erro={}", e.getMessage());
                    }
                });
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            try {
                if (topics.commands().equals(topic)) {
                    CommandOutcome outcome = processor.process(new String(message.getPayload(), StandardCharsets.UTF_8));
                    publishEvent(outcomeEvent(outcome));
                }
            } catch (MqttException | RuntimeException e) {
                log.error("controlplane.mqtt.message_error topic={} erro={}", topic, e.getMessage(), e);
            } finally {
                try {
                    client.messageArrivedComplete(message.getId(), message.getQos());
                } catch (MqttException e) {
                    log.warn("controlplane.mqtt.ack_failed erro={}", e.getMessage());
                }
            }
        }

        @Override
        public void disconnected(MqttDisconnectResponse response) {
            log.warn("controlplane.mqtt.disconnected motivo={}", response == null ? null : response.getReasonString());
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            log.warn("controlplane.mqtt.error erro={}", exception.getMessage());
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
            // nada: quem precisa da confirmação espera o token
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
            // autenticação estendida não é usada
        }
    }

    private static void closeQuietly(MqttAsyncClient c) {
        try {
            c.close(true);
        } catch (MqttException e) {
            log.debug("controlplane.mqtt.close_error erro={}", e.getMessage());
        }
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
