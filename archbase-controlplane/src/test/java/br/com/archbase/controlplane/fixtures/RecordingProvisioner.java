package br.com.archbase.controlplane.fixtures;

import br.com.archbase.controlplane.provisioning.PresetDescriptor;
import br.com.archbase.controlplane.provisioning.ProvisionResult;
import br.com.archbase.controlplane.provisioning.TenantProvisioner;
import br.com.archbase.controlplane.provisioning.TenantSpec;
import br.com.archbase.ddd.context.ArchbaseTenantContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** {@link TenantProvisioner} de teste: registra cada chamada e pode ser instruído a falhar. */
public class RecordingProvisioner implements TenantProvisioner {

    /** Uma chamada: operação, tenant e o tenant que estava no contexto durante a chamada. */
    public record Call(String operation, String tenantId, String contextTenant, TenantSpec spec) {
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> failWith = new AtomicReference<>();

    public List<Call> calls() {
        return calls;
    }

    public long count(String operation, String tenantId) {
        return calls.stream().filter(c -> c.operation().equals(operation) && c.tenantId().equals(tenantId)).count();
    }

    /** Próximas chamadas falham com este motivo; {@code null} volta ao normal. */
    public void failWith(String reason) {
        failWith.set(reason);
    }

    public void reset() {
        calls.clear();
        failWith.set(null);
    }

    private ProvisionResult record(String operation, String tenantId, TenantSpec spec) {
        calls.add(new Call(operation, tenantId, ArchbaseTenantContext.getTenantId(), spec));
        String reason = failWith.get();
        return reason == null ? ProvisionResult.ok(Map.of("operation", operation)) : ProvisionResult.failed(reason);
    }

    @Override
    public ProvisionResult provision(TenantSpec spec) {
        return record("provision", spec.tenantId(), spec);
    }

    @Override
    public ProvisionResult update(TenantSpec spec) {
        return record("update", spec.tenantId(), spec);
    }

    @Override
    public ProvisionResult suspend(String tenantId) {
        return record("suspend", tenantId, null);
    }

    @Override
    public ProvisionResult resume(String tenantId) {
        return record("resume", tenantId, null);
    }

    @Override
    public ProvisionResult deprovision(String tenantId) {
        return record("deprovision", tenantId, null);
    }

    @Override
    public ProvisionResult reset(String tenantId) {
        return record("reset", tenantId, null);
    }

    @Override
    public List<PresetDescriptor> presets() {
        return List.of(new PresetDescriptor("varejo", "1", "Varejo", "Preset de varejo", List.of("filiais")));
    }
}
