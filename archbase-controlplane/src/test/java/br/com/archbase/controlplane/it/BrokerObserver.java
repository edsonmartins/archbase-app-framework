package br.com.archbase.controlplane.it;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.awaitility.Awaitility;

/** O lado do Control Plane no broker: publica comandos e observa eventos e status das instâncias. */
final class BrokerObserver implements AutoCloseable {

    record Received(String topic, JsonNode body, boolean retained) {
    }

    private final MqttAsyncClient client;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private final JsonMapper json = JsonMapper.builder().build();

    BrokerObserver(String uri) throws MqttException {
        client = new MqttAsyncClient(uri, "observador-" + UUID.randomUUID(), new MemoryPersistence());
        client.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                received.add(new Received(topic, json.readTree(new String(message.getPayload(),
                        StandardCharsets.UTF_8)), message.isRetained()));
            }

            @Override
            public void disconnected(MqttDisconnectResponse r) {
            }

            @Override
            public void mqttErrorOccurred(MqttException e) {
            }

            @Override
            public void deliveryComplete(IMqttToken t) {
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
            }
        });
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        client.connect(options).waitForCompletion(10_000);
        client.subscribe("cp/v1/inst/+/evt", 1).waitForCompletion(10_000);
        client.subscribe("cp/v1/inst/+/status", 1).waitForCompletion(10_000);
    }

    void publishCommand(String instanceId, String signedCommand) throws MqttException {
        MqttMessage message = new MqttMessage(signedCommand.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        client.publish("cp/v1/inst/" + instanceId + "/cmd", message).waitForCompletion(10_000);
    }

    Received await(Predicate<Received> condition, Duration timeout) {
        Awaitility.await().atMost(timeout).until(() -> received.stream().anyMatch(condition));
        return received.stream().filter(condition).findFirst().orElseThrow();
    }

    List<Received> matching(Predicate<Received> condition) {
        return received.stream().filter(condition).toList();
    }

    static Predicate<Received> result(String commandId) {
        return r -> r.topic().endsWith("/evt") && "command.result".equals(r.body().path("event").asString(null))
                && commandId.equals(r.body().path("commandId").asString(null));
    }

    static Predicate<Received> status(String instanceId, String state) {
        return r -> r.topic().equals("cp/v1/inst/" + instanceId + "/status")
                && state.equals(r.body().path("state").asString(null));
    }

    @Override
    public void close() {
        try {
            client.disconnect().waitForCompletion(2000);
            client.close();
        } catch (MqttException ignored) {
            // fim de teste
        }
    }
}
