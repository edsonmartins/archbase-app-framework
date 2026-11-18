package br.com.archbase.controlplane.state;

import br.com.archbase.controlplane.license.InstanceLicense;
import br.com.archbase.controlplane.license.LicenseRejectedException;
import br.com.archbase.controlplane.license.LicenseStatus;
import br.com.archbase.controlplane.license.LicenseVerifier;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado do Control Plane nesta instância: licença e último snapshot de cada tenant. Lido do banco na subida e
 * mantido em memória — as consultas por requisição ({@code can}, {@code tenantState}, modo restrito) não vão ao
 * banco. Atualizado pelos comandos do Control Plane (Fase C) e por {@link #applyLicense}/{@link #applyTenant}.
 * <p>
 * Com o Control Plane fora do ar, o último estado continua valendo; a licença decide até quando, por
 * {@code exp + grace-period}.
 *
 * @since 3.4.6
 */
public class ControlPlaneState {

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneState.class);

    private final ControlPlaneStateStore store;
    private final LicenseVerifier verifier;
    private final Duration gracePeriod;
    private final Clock clock;
    private final Map<String, TenantSnapshot> tenants = new ConcurrentHashMap<>();
    private volatile InstanceLicense license;

    public ControlPlaneState(ControlPlaneStateStore store, LicenseVerifier verifier, Duration gracePeriod,
                             Clock clock) {
        this.store = store;
        this.verifier = verifier;
        this.gracePeriod = gracePeriod;
        this.clock = clock;
    }

    /** Carrega do banco e, se houver, do arquivo de licença. Nunca falha a subida. */
    public void load(String licensePath) {
        tenants.putAll(store.loadTenants());
        store.loadLicenseToken().ifPresent(token -> {
            try {
                license = verifier.verify(token);
            } catch (LicenseRejectedException e) {
                log.error("controlplane.license.stored_invalid erro={}", e.getMessage());
            }
        });
        if (licensePath != null && !licensePath.isBlank() && Files.isRegularFile(Path.of(licensePath))) {
            try {
                applyLicense(Files.readString(Path.of(licensePath), StandardCharsets.UTF_8).trim());
            } catch (IOException | LicenseRejectedException e) {
                log.error("controlplane.license.file_rejected path={} erro={}", licensePath, e.getMessage());
            }
        }
        log.info("controlplane.state.loaded license={} expiresAt={} tenants={}", licenseStatus(),
                license == null ? null : license.expiresAt(), tenants.size());
    }

    // ------------------------------------------------------------------ licença

    /**
     * Verifica e aplica a licença. Só substitui a atual se vencer depois dela: licença antiga reentregue não encurta a
     * validade.
     *
     * @return se a licença foi aplicada
     * @throws LicenseRejectedException assinatura, instância ou produto não conferem
     */
    public synchronized boolean applyLicense(String token) {
        InstanceLicense candidate = verifier.verify(token);
        if (license != null && !candidate.expiresAt().isAfter(license.expiresAt())) {
            log.debug("controlplane.license.ignored expiresAt={} atual={}", candidate.expiresAt(), license.expiresAt());
            return false;
        }
        store.saveLicenseToken(token);
        license = candidate;
        log.info("controlplane.license.applied expiresAt={} status={}", candidate.expiresAt(), licenseStatus());
        return true;
    }

    public Optional<InstanceLicense> license() {
        return Optional.ofNullable(license);
    }

    /** Sem licença conta como {@link LicenseStatus#EXPIRED}. */
    public LicenseStatus licenseStatus() {
        InstanceLicense current = license;
        return current == null ? LicenseStatus.EXPIRED : current.statusAt(clock.instant(), gracePeriod);
    }

    public Optional<Instant> graceEndsAt() {
        return license().map(l -> l.expiresAt().plus(gracePeriod));
    }

    // ------------------------------------------------------------------ tenants

    /**
     * Aplica o snapshot de um tenant. Versão menor ou igual à atual é ignorada.
     *
     * @return se o snapshot foi aplicado
     */
    public synchronized boolean applyTenant(TenantSnapshot snapshot) {
        TenantSnapshot current = tenants.get(snapshot.tenantId());
        if (current != null && snapshot.version() <= current.version()) {
            log.debug("controlplane.tenant.ignored tnt={} versao={} atual={}", snapshot.tenantId(), snapshot.version(),
                    current.version());
            return false;
        }
        store.saveTenant(snapshot);
        tenants.put(snapshot.tenantId(), snapshot);
        log.info("controlplane.tenant.applied tnt={} versao={} state={}", snapshot.tenantId(), snapshot.version(),
                snapshot.state());
        return true;
    }

    public Optional<TenantSnapshot> tenant(String tenantId) {
        return tenantId == null ? Optional.empty() : Optional.ofNullable(tenants.get(tenantId));
    }

    /** Tenants que esta instância atende (os que têm snapshot). */
    public java.util.Set<String> tenantIds() {
        return java.util.Set.copyOf(tenants.keySet());
    }

    /** Esquece o tenant (desprovisionado). */
    public synchronized void removeTenant(String tenantId) {
        store.deleteTenant(tenantId);
        tenants.remove(tenantId);
        log.info("controlplane.tenant.removed tnt={}", tenantId);
    }

    // ------------------------------------------------------------------ consumo

    public void recordUsage(String tenantId, String key, long amount) {
        store.recordUsage(tenantId, key, amount, clock.instant());
    }

    public long unreportedUsage(String tenantId, String key) {
        return store.unreportedUsage(tenantId, key);
    }

    public Clock clock() {
        return clock;
    }
}
