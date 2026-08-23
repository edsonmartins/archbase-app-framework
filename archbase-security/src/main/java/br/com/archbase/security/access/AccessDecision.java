package br.com.archbase.security.access;

import java.util.List;

/**
 * O resultado de uma avaliação de acesso — e, principalmente, <b>o porquê</b>.
 *
 * <p>Antes do core, a decisão era um {@code boolean}. A informação de qual grupo ou perfil concedeu
 * o acesso já vinha da consulta e era descartada por um {@code anyMatch}, o que obrigava quem
 * investigava um acesso a abrir grupo por grupo no admin. Guardar a razão é o que torna possíveis,
 * sem consulta nova, a tela de efetivo do usuário e a simulação.
 *
 * @param grantedBy     id do perfil, grupo ou usuário que concedeu; {@code null} quando negado ou
 *                      quando a concessão veio da flag de administrador
 * @param grantedByName nome do concedente, para exibição
 * @param chain         a sequência de portões avaliados, na ordem
 */
public record AccessDecision(
        boolean allowed,
        Gate deniedAt,
        String reasonCode,
        String message,
        String grantedBy,
        String grantedByName,
        List<GateOutcome> chain) {

    public AccessDecision {
        chain = chain == null ? List.of() : List.copyOf(chain);
    }

    public static AccessDecision granted(String reasonCode, String message,
                                         String grantedBy, String grantedByName,
                                         List<GateOutcome> chain) {
        return new AccessDecision(true, null, reasonCode, message, grantedBy, grantedByName, chain);
    }

    public static AccessDecision denied(Gate deniedAt, String reasonCode, String message,
                                        List<GateOutcome> chain) {
        return new AccessDecision(false, deniedAt, reasonCode, message, null, null, chain);
    }

    /** Resumo de uma linha, para log. */
    public String summary() {
        return allowed
                ? "PERMITE (" + reasonCode + ")" + (grantedByName == null ? "" : " via " + grantedByName)
                : "NEGA em " + deniedAt + " (" + reasonCode + ")";
    }
}
