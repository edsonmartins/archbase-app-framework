package br.com.archbase.security.access;

/**
 * O que aconteceu em um portão. A sequência deles forma a cadeia de
 * {@link AccessDecision#chain()}.
 *
 * @param gate       o portão avaliado
 * @param passed     se o acesso seguiu adiante
 * @param reasonCode código estável, de {@link AccessReasonCodes}
 * @param detail     explicação legível — o que foi comparado com o quê
 */
public record GateOutcome(Gate gate, boolean passed, String reasonCode, String detail) {

    public static GateOutcome passed(Gate gate, String detail) {
        return new GateOutcome(gate, true, null, detail);
    }

    public static GateOutcome passed(Gate gate, String reasonCode, String detail) {
        return new GateOutcome(gate, true, reasonCode, detail);
    }

    public static GateOutcome denied(Gate gate, String reasonCode, String detail) {
        return new GateOutcome(gate, false, reasonCode, detail);
    }
}
