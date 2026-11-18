package br.com.archbase.controlplane.provisioning;

import java.util.List;

/**
 * O que cada comando de provisionamento significa para o banco do produto. O módulo cuida do transporte (MQTT,
 * assinatura, idempotência, confirmação); o produto implementa só isto, como um bean.
 * <p>
 * Cada método roda numa transação e com {@code ArchbaseTenantContext} no tenant do comando. Devolver
 * {@link ProvisionResult#failed} (ou lançar exceção) desfaz a transação e publica {@code failed} com o motivo: o
 * Control Plane reenvia e alerta. O mesmo comando nunca é aplicado duas vezes — repetição é descartada antes de
 * chegar aqui.
 *
 * @since 3.4.6
 */
public interface TenantProvisioner {

    /** Cria o tenant a partir do preset. */
    ProvisionResult provision(TenantSpec spec);

    /** Edição, entitlements, assentos. */
    ProvisionResult update(TenantSpec spec);

    ProvisionResult suspend(String tenantId);

    ProvisionResult resume(String tenantId);

    /** Exporta e apaga. */
    ProvisionResult deprovision(String tenantId);

    /** Demo: volta ao preset. */
    ProvisionResult reset(String tenantId);

    /** Catálogo de presets do produto, publicado no primeiro registro e a cada versão nova. */
    List<PresetDescriptor> presets();
}
