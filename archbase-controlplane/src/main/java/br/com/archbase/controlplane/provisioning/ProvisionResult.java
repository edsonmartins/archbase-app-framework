package br.com.archbase.controlplane.provisioning;

import java.util.Map;

/**
 * Resultado de uma operação do {@link TenantProvisioner}.
 *
 * @param success se a operação foi aplicada
 * @param reason  motivo da falha, publicado ao Control Plane
 * @param details dados livres devolvidos ao Control Plane junto com {@code applied} (ex.: arquivo da exportação)
 * @since 3.4.6
 */
public record ProvisionResult(boolean success, String reason, Map<String, Object> details) {

    public ProvisionResult {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static ProvisionResult ok() {
        return new ProvisionResult(true, null, Map.of());
    }

    public static ProvisionResult ok(Map<String, Object> details) {
        return new ProvisionResult(true, null, details);
    }

    public static ProvisionResult failed(String reason) {
        return new ProvisionResult(false, reason, Map.of());
    }
}
