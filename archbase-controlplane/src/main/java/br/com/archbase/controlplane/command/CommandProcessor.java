package br.com.archbase.controlplane.command;

import br.com.archbase.controlplane.provisioning.ProvisionResult;
import br.com.archbase.controlplane.provisioning.TenantProvisioner;
import br.com.archbase.controlplane.provisioning.TenantSpec;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import br.com.archbase.controlplane.tenant.TenantState;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Aplica um comando do Control Plane, independentemente do transporte (MQTT ou outro):
 * <ol>
 * <li>verifica a assinatura — comando inválido não chama nada e volta {@code REJECTED};</li>
 * <li>numa transação, registra o id em {@code CONTROLPLANE_COMANDO} — repetição volta {@code DUPLICATE} sem efeito —
 * e chama o {@link TenantProvisioner} (ou aplica o comando do próprio módulo);</li>
 * <li>falha do produto desfaz a transação, inclusive o registro do id: o Control Plane reenvia e o comando roda de
 * novo;</li>
 * <li>só depois do commit atualiza o cache de estado do tenant.</li>
 * </ol>
 *
 * @since 3.4.6
 */
public class CommandProcessor {

    private static final Logger log = LoggerFactory.getLogger(CommandProcessor.class);
    private static final Logger security = LoggerFactory.getLogger("br.com.archbase.controlplane.security");

    private final CommandVerifier verifier;
    private final ControlPlaneStateStore store;
    private final ControlPlaneState state;
    private final ObjectProvider<TenantProvisioner> provisioner;
    private final TransactionTemplate tx;

    public CommandProcessor(CommandVerifier verifier, ControlPlaneStateStore store, ControlPlaneState state,
                            ObjectProvider<TenantProvisioner> provisioner, TransactionTemplate tx) {
        this.verifier = verifier;
        this.store = store;
        this.state = state;
        this.provisioner = provisioner;
        this.tx = tx;
    }

    public CommandOutcome process(String payload) {
        ControlPlaneCommand command;
        try {
            command = verifier.verify(payload);
        } catch (CommandRejectedException e) {
            String id = verifier.unverifiedId(payload);
            security.warn("controlplane.command.rejected commandId={} motivo={}", id, e.getMessage());
            return new CommandOutcome(id, null, CommandOutcome.Status.REJECTED, e.getMessage(), null);
        }

        String type = command.type();
        if (!CommandTypes.PRODUCT.contains(type) && !CommandTypes.MODULE.contains(type)) {
            log.warn("controlplane.command.failed commandId={} cmd={} motivo=tipo desconhecido", command.id(), type);
            return failed(command, "tipo de comando desconhecido: " + type);
        }
        TenantProvisioner product = null;
        if (CommandTypes.PRODUCT.contains(type)) {
            product = provisioner.getIfAvailable();
            if (product == null) {
                return failed(command, "o produto não registrou um TenantProvisioner");
            }
        }

        String tenantId = tenantOf(command);
        String previousTenant = ArchbaseTenantContext.getTenantId();
        try {
            if (tenantId != null) {
                ArchbaseTenantContext.setTenantId(tenantId);
            }
            TenantProvisioner target = product;
            CommandOutcome outcome = tx.execute(status -> {
                if (!store.recordCommand(command.id(), type)) {
                    return new CommandOutcome(command.id(), type, CommandOutcome.Status.DUPLICATE, null, null);
                }
                ProvisionResult result = target != null ? callProduct(target, command, tenantId)
                        : applyModuleCommand(command, tenantId);
                if (result == null || !result.success()) {
                    status.setRollbackOnly();
                    return new CommandOutcome(command.id(), type, CommandOutcome.Status.FAILED,
                            result == null ? "o TenantProvisioner devolveu null" : result.reason(), null);
                }
                return new CommandOutcome(command.id(), type, CommandOutcome.Status.APPLIED, null, result.details());
            });
            if (outcome != null && outcome.status() == CommandOutcome.Status.APPLIED && product != null) {
                updateCacheAfterProductCommand(command, tenantId);
            }
            logOutcome(outcome, tenantId);
            return outcome;
        } catch (RuntimeException e) {
            log.error("controlplane.command.failed commandId={} cmd={} tnt={} erro={}", command.id(), type, tenantId,
                    e.getMessage(), e);
            return failed(command, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            if (previousTenant == null) {
                ArchbaseTenantContext.clear();
            } else {
                ArchbaseTenantContext.setTenantId(previousTenant);
            }
        }
    }

    // ------------------------------------------------------------------ produto

    private ProvisionResult callProduct(TenantProvisioner product, ControlPlaneCommand command, String tenantId) {
        requireTenant(command, tenantId);
        return switch (command.type()) {
            case CommandTypes.TENANT_PROVISION -> product.provision(spec(command, tenantId));
            case CommandTypes.TENANT_UPDATE -> product.update(spec(command, tenantId));
            case CommandTypes.TENANT_SUSPEND -> product.suspend(tenantId);
            case CommandTypes.TENANT_RESUME -> product.resume(tenantId);
            case CommandTypes.TENANT_DEPROVISION -> product.deprovision(tenantId);
            case CommandTypes.TENANT_RESET -> product.reset(tenantId);
            default -> throw new IllegalStateException(command.type());
        };
    }

    /** O cache só muda depois do commit: se o produto falhar, a instância não passa a anunciar o que não aplicou. */
    private void updateCacheAfterProductCommand(ControlPlaneCommand command, String tenantId) {
        Optional<TenantSnapshot> current = state.tenant(tenantId);
        switch (command.type()) {
            case CommandTypes.TENANT_PROVISION, CommandTypes.TENANT_UPDATE -> {
                TenantSpec spec = spec(command, tenantId);
                TenantState tenantState = current.map(TenantSnapshot::state)
                        .filter(s -> s != TenantState.UNKNOWN).orElse(TenantState.ACTIVE);
                state.applyTenant(new TenantSnapshot(tenantId, nextVersion(command, current), tenantState,
                        current.map(TenantSnapshot::suspendedAt).orElse(null), spec.edition(), spec.entitlements(),
                        current.map(TenantSnapshot::usageBaseline).orElse(Map.of()),
                        spec.seatHolders() != null ? spec.seatHolders()
                                : current.map(TenantSnapshot::seatHolders).orElse(null),
                        Instant.now()));
            }
            case CommandTypes.TENANT_SUSPEND -> state.applyTenant(withState(current, tenantId, command,
                    TenantState.SUSPENDED, command.issuedAt() != null ? command.issuedAt() : Instant.now()));
            case CommandTypes.TENANT_RESUME -> state.applyTenant(withState(current, tenantId, command,
                    TenantState.ACTIVE, null));
            case CommandTypes.TENANT_DEPROVISION -> state.removeTenant(tenantId);
            default -> {
                // reset: estado comercial não muda
            }
        }
    }

    private static TenantSnapshot withState(Optional<TenantSnapshot> current, String tenantId,
                                            ControlPlaneCommand command, TenantState newState, Instant suspendedAt) {
        return new TenantSnapshot(tenantId, nextVersion(command, current), newState, suspendedAt,
                current.map(TenantSnapshot::edition).orElse(null),
                current.map(TenantSnapshot::entitlements).orElse(Map.of()),
                current.map(TenantSnapshot::usageBaseline).orElse(Map.of()),
                current.map(TenantSnapshot::seatHolders).orElse(null),
                Instant.now());
    }

    // ------------------------------------------------------------------ módulo

    private ProvisionResult applyModuleCommand(ControlPlaneCommand command, String tenantId) {
        JsonNode data = command.data();
        switch (command.type()) {
            case CommandTypes.LICENSE_RENEWED -> {
                String license = data.path("license").asString(null);
                if (license == null) {
                    return ProvisionResult.failed("license.renewed sem data.license");
                }
                state.applyLicense(license);
                return ProvisionResult.ok();
            }
            case CommandTypes.ENTITLEMENTS_CHANGED -> {
                requireTenant(command, tenantId);
                Optional<TenantSnapshot> current = state.tenant(tenantId);
                state.applyTenant(new TenantSnapshot(tenantId, nextVersion(command, current),
                        data.hasNonNull("state") ? TenantState.parse(data.get("state").asString())
                                : current.map(TenantSnapshot::state).orElse(TenantState.ACTIVE),
                        current.map(TenantSnapshot::suspendedAt).orElse(null),
                        data.hasNonNull("edition") ? data.get("edition").asString()
                                : current.map(TenantSnapshot::edition).orElse(null),
                        data.has("entitlements") ? longs(data.get("entitlements"))
                                : current.map(TenantSnapshot::entitlements).orElse(Map.of()),
                        data.has("usageBaseline") ? longs(data.get("usageBaseline"))
                                : current.map(TenantSnapshot::usageBaseline).orElse(Map.of()),
                        current.map(TenantSnapshot::seatHolders).orElse(null),
                        Instant.now()));
                return ProvisionResult.ok();
            }
            case CommandTypes.MEMBERSHIP_CHANGED -> {
                requireTenant(command, tenantId);
                Optional<TenantSnapshot> current = state.tenant(tenantId);
                state.applyTenant(new TenantSnapshot(tenantId, nextVersion(command, current),
                        current.map(TenantSnapshot::state).orElse(TenantState.ACTIVE),
                        current.map(TenantSnapshot::suspendedAt).orElse(null),
                        current.map(TenantSnapshot::edition).orElse(null),
                        current.map(TenantSnapshot::entitlements).orElse(Map.of()),
                        current.map(TenantSnapshot::usageBaseline).orElse(Map.of()),
                        strings(data.path("seatHolders")),
                        Instant.now()));
                return ProvisionResult.ok();
            }
            default -> throw new IllegalStateException(command.type());
        }
    }

    // ------------------------------------------------------------------ apoio

    /**
     * {@code data.version} quando o Control Plane a declara — aí comando atrasado com versão menor é ignorado. Sem
     * ela, a ordem de chegada (MQTT entrega em ordem por tópico): a versão atual + 1.
     */
    private static long nextVersion(ControlPlaneCommand command, Optional<TenantSnapshot> current) {
        if (command.explicitVersion().isPresent()) {
            return command.explicitVersion().getAsLong();
        }
        return current.map(TenantSnapshot::version).orElse(0L) + 1;
    }

    private static String tenantOf(ControlPlaneCommand command) {
        JsonNode data = command.data();
        if (data.hasNonNull("tenantId")) {
            return data.get("tenantId").asString();
        }
        return null;
    }

    private static void requireTenant(ControlPlaneCommand command, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(command.type() + " sem data.tenantId");
        }
    }

    private static TenantSpec spec(ControlPlaneCommand command, String tenantId) {
        JsonNode d = command.data();
        Map<String, Object> parameters = new LinkedHashMap<>();
        d.path("presetParameters").properties().forEach(e -> parameters.put(e.getKey(), scalar(e.getValue())));
        JsonNode admin = d.path("admin");
        return new TenantSpec(tenantId,
                d.path("clientId").asString(null),
                d.path("edition").asString(null),
                d.path("editionVersion").asString(null),
                longs(d.path("entitlements")),
                d.path("preset").asString(null),
                d.path("presetVersion").asString(null),
                parameters,
                admin.path("id").asString(null),
                admin.path("email").asString(null),
                d.has("seatHolders") ? strings(d.get("seatHolders")) : null);
    }

    private static Map<String, Long> longs(JsonNode node) {
        Map<String, Long> result = new LinkedHashMap<>();
        node.properties().forEach(e -> result.put(e.getKey(),
                e.getValue().isBoolean() ? (e.getValue().asBoolean() ? 1L : 0L) : e.getValue().asLong()));
        return result;
    }

    private static Map<String, String> strings(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();
        node.properties().forEach(e -> result.put(e.getKey(), e.getValue().asString()));
        return result;
    }

    private static Object scalar(JsonNode node) {
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        return node.isString() ? node.asString() : node.toString();
    }

    private static CommandOutcome failed(ControlPlaneCommand command, String reason) {
        return new CommandOutcome(command.id(), command.type(), CommandOutcome.Status.FAILED, reason, null);
    }

    private static void logOutcome(CommandOutcome outcome, String tenantId) {
        if (outcome == null) {
            return;
        }
        switch (outcome.status()) {
            case APPLIED -> log.info("controlplane.command.applied commandId={} cmd={} tnt={}", outcome.commandId(),
                    outcome.type(), tenantId);
            case DUPLICATE -> log.info("controlplane.command.duplicate commandId={} cmd={} tnt={}",
                    outcome.commandId(), outcome.type(), tenantId);
            case FAILED -> log.warn("controlplane.command.failed commandId={} cmd={} tnt={} motivo={}",
                    outcome.commandId(), outcome.type(), tenantId, outcome.reason());
            default -> {
            }
        }
    }
}
