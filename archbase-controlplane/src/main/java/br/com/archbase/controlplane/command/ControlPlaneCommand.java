package br.com.archbase.controlplane.command;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Comando do Control Plane com a assinatura já verificada.
 *
 * @param id       {@code jti}: identidade do comando, usada na idempotência
 * @param type     {@code cmd}: {@code tenant.provision}, {@code license.renewed}...
 * @param issuedAt {@code iat}
 * @param data     corpo do comando
 * @since 3.4.6
 */
public record ControlPlaneCommand(String id, String type, Instant issuedAt, JsonNode data) {

    /**
     * Versão do estado declarada pelo Control Plane em {@code data.version}, se houver. Sem ela, quem aplica usa a
     * ordem de chegada — o {@code iat} não serve, porque tem precisão de segundos e dois comandos do mesmo segundo
     * empatariam.
     */
    public java.util.OptionalLong explicitVersion() {
        if (data != null && data.hasNonNull("version")) {
            return java.util.OptionalLong.of(data.get("version").asLong());
        }
        return java.util.OptionalLong.empty();
    }
}
