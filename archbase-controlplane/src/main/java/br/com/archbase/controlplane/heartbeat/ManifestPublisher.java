package br.com.archbase.controlplane.heartbeat;

import br.com.archbase.controlplane.mqtt.ControlPlaneMqttClient;
import br.com.archbase.controlplane.provisioning.FeatureManifestProvider;
import br.com.archbase.controlplane.provisioning.PresetDescriptor;
import br.com.archbase.controlplane.provisioning.TenantProvisioner;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;

/**
 * Publica os presets e o manifesto de features no primeiro registro e a cada versão nova do produto. A última versão
 * publicada fica em {@code CONTROLPLANE_META}; ela só é gravada depois que o broker confirma o recebimento.
 *
 * @since 3.4.6
 */
public class ManifestPublisher {

    static final String META_KEY = "manifest.version";

    private static final Logger log = LoggerFactory.getLogger(ManifestPublisher.class);

    private final ControlPlaneMqttClient mqtt;
    private final ControlPlaneStateStore store;
    private final ObjectProvider<TenantProvisioner> provisioner;
    private final ObjectProvider<FeatureManifestProvider> features;
    private final String product;
    private final String productVersion;
    private final JsonMapper json = JsonMapper.builder().build();

    public ManifestPublisher(ControlPlaneMqttClient mqtt, ControlPlaneStateStore store,
                             ObjectProvider<TenantProvisioner> provisioner,
                             ObjectProvider<FeatureManifestProvider> features, String product, String productVersion) {
        this.mqtt = mqtt;
        this.store = store;
        this.provisioner = provisioner;
        this.features = features;
        this.product = product;
        this.productVersion = productVersion;
    }

    /** Publica se a versão mudou desde a última publicação confirmada. */
    public synchronized boolean publishIfChanged() {
        if (store.meta(META_KEY).filter(productVersion::equals).isPresent()) {
            return false;
        }
        try {
            mqtt.publishEvent(manifest()).waitForCompletion(10_000);
            store.setMeta(META_KEY, productVersion);
            log.info("controlplane.manifest.published version={}", productVersion);
            return true;
        } catch (Exception e) {
            log.warn("controlplane.manifest.publish_failed version={} erro={} — nova tentativa na próxima conexão",
                    productVersion, e.getMessage());
            return false;
        }
    }

    ObjectNode manifest() {
        ObjectNode node = json.createObjectNode();
        node.put("event", "manifest");
        node.put("product", product);
        node.put("version", productVersion);
        ArrayNode presets = node.putArray("presets");
        TenantProvisioner p = provisioner.getIfAvailable();
        List<PresetDescriptor> list = p == null ? List.of() : p.presets();
        for (PresetDescriptor preset : list) {
            presets.add(json.valueToTree(preset));
        }
        ArrayNode featureArray = node.putArray("features");
        FeatureManifestProvider f = features.getIfAvailable();
        if (f != null) {
            f.features().forEach(feature -> featureArray.add(json.valueToTree(feature)));
        }
        node.put("at", Instant.now().toString());
        return node;
    }
}
