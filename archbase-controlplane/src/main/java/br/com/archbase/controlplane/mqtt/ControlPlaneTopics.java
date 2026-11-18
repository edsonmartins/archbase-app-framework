package br.com.archbase.controlplane.mqtt;

/**
 * Tópicos da instância no broker do Control Plane.
 *
 * @since 3.4.6
 */
public record ControlPlaneTopics(String commands, String events, String status) {

    public static ControlPlaneTopics forInstance(String instanceId) {
        String base = "cp/v1/inst/" + instanceId + "/";
        return new ControlPlaneTopics(base + "cmd", base + "evt", base + "status");
    }
}
