package br.com.archbase.controlplane.api;

import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entitlements de um tenant, a partir do cache local — sem chamada ao Control Plane.
 *
 * @since 3.4.6
 */
public class Entitlements {

    private static final Logger log = LoggerFactory.getLogger(Entitlements.class);

    private final ControlPlaneState state;
    private final String tenantId;

    public Entitlements(ControlPlaneState state, String tenantId) {
        this.state = state;
        this.tenantId = tenantId;
    }

    /** Se o tenant tem o entitlement (booleano verdadeiro, ou valor numérico maior que zero). */
    public boolean can(String key) {
        return value(key) > 0;
    }

    /** Valor do entitlement; 0 se o tenant não o tem ou se não há informação do tenant. */
    public long value(String key) {
        return state.tenant(tenantId).map(TenantSnapshot::entitlements).map(e -> e.getOrDefault(key, 0L)).orElse(0L);
    }

    /**
     * Registra consumo de um entitlement metered (créditos, chamadas…). O registro é local e vai para o Control Plane
     * no heartbeat; funciona com o Control Plane fora do ar. Estouro não é recusado: volta em
     * {@link Usage#exceeded()} e fica registrado em log, para o produto decidir o que fazer.
     */
    public Usage consume(String key, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Consumo negativo: " + amount);
        }
        if (tenantId == null) {
            throw new IllegalStateException("Consumo sem tenant no contexto");
        }
        state.recordUsage(tenantId, key, amount);
        long baseline = state.tenant(tenantId).map(s -> s.usageBaseline().getOrDefault(key, 0L)).orElse(0L);
        long used = baseline + state.unreportedUsage(tenantId, key);
        long limit = value(key);
        boolean exceeded = used > limit;
        if (exceeded) {
            state.metrics().usageExceeded(key);
            log.warn("controlplane.usage.exceeded tnt={} chave={} usado={} limite={}", tenantId, key, used, limit);
        }
        return new Usage(key, amount, used, limit, Math.max(0, limit - used), exceeded);
    }
}
