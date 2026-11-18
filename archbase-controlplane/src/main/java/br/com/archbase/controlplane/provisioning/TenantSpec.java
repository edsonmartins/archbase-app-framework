package br.com.archbase.controlplane.provisioning;

import java.util.Map;

/**
 * Especificação do tenant enviada pelo Control Plane em {@code tenant.provision} e {@code tenant.update}.
 *
 * @param tenantId         id do tenant
 * @param clientId         cliente dono do tenant
 * @param edition          edição contratada
 * @param editionVersion   versão da edição
 * @param entitlements     entitlements (booleano = 1/0)
 * @param preset           preset de criação
 * @param presetVersion    versão do preset
 * @param presetParameters parâmetros do preset
 * @param adminUserId      admin inicial: id no Control Plane
 * @param adminEmail       admin inicial: e-mail
 * @param seatHolders      quem tem assento (id do usuário → id do assento), ou {@code null} se não informado
 * @since 3.4.6
 */
public record TenantSpec(
        String tenantId,
        String clientId,
        String edition,
        String editionVersion,
        Map<String, Long> entitlements,
        String preset,
        String presetVersion,
        Map<String, Object> presetParameters,
        String adminUserId,
        String adminEmail,
        Map<String, String> seatHolders) {

    public TenantSpec {
        entitlements = entitlements == null ? Map.of() : Map.copyOf(entitlements);
        presetParameters = presetParameters == null ? Map.of() : Map.copyOf(presetParameters);
        seatHolders = seatHolders == null ? null : Map.copyOf(seatHolders);
    }
}
