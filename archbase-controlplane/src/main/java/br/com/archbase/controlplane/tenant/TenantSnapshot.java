package br.com.archbase.controlplane.tenant;

import java.time.Instant;
import java.util.Map;

/**
 * O que o Control Plane disse por último sobre um tenant: estado, edição, entitlements e assentos.
 * <p>
 * <b>Contrato assumido — confirmar com as ADRs 0010 e 0014.</b>
 * <ul>
 * <li>{@code entitlements}: chave → valor. Booleano vira 1/0; limite e crédito são numéricos.</li>
 * <li>{@code usageBaseline}: consumo metered já contabilizado pelo Control Plane, por chave. O consumo local ainda
 * não reportado soma em cima dele.</li>
 * <li>{@code seatHolders}: id do usuário no Control Plane → id do assento. {@code null} quando o Control Plane não
 * informou assentos — aí vale só o claim {@code seat} do token.</li>
 * <li>{@code version}: crescente; atualização com versão menor ou igual à atual é ignorada (comando repetido ou fora de
 * ordem).</li>
 * </ul>
 *
 * @since 3.4.6
 */
public record TenantSnapshot(
        String tenantId,
        long version,
        TenantState state,
        Instant suspendedAt,
        String edition,
        Map<String, Long> entitlements,
        Map<String, Long> usageBaseline,
        Map<String, String> seatHolders,
        Instant updatedAt) {

    public TenantSnapshot {
        entitlements = entitlements == null ? Map.of() : Map.copyOf(entitlements);
        usageBaseline = usageBaseline == null ? Map.of() : Map.copyOf(usageBaseline);
        seatHolders = seatHolders == null ? null : Map.copyOf(seatHolders);
        state = state == null ? TenantState.UNKNOWN : state;
    }

    /** Se o Control Plane informou quem tem assento. */
    public boolean seatsKnown() {
        return seatHolders != null;
    }
}
