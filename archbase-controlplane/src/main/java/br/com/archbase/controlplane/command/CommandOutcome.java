package br.com.archbase.controlplane.command;

import java.util.Map;

/**
 * Resultado do processamento de um comando, publicado ao Control Plane.
 *
 * @since 3.4.6
 */
public record CommandOutcome(String commandId, String type, Status status, String reason,
                             Map<String, Object> details) {

    public enum Status {
        /** Aplicado agora. */
        APPLIED,
        /** Já tinha sido aplicado: repetição descartada (publicado como {@code applied}, para o Control Plane parar
         * de reenviar). */
        DUPLICATE,
        /** O produto (ou o módulo) não conseguiu aplicar: nada foi gravado, o Control Plane reenvia. */
        FAILED,
        /** Assinatura inválida ou comando de outra instância: nada foi chamado. */
        REJECTED
    }

    public CommandOutcome {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String wireStatus() {
        return switch (status) {
            case APPLIED, DUPLICATE -> "applied";
            case FAILED -> "failed";
            case REJECTED -> "rejected";
        };
    }
}
