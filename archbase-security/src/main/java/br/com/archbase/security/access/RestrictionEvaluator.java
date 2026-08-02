package br.com.archbase.security.access;

/**
 * Avalia uma tranca de um tipo. Um por {@link RestrictionKind}.
 *
 * <p>A lógica de cada anotação continua separada — cada uma tem sua ordem de checagem e suas
 * particularidades, e uniformizá-las mudaria decisão. O que se unifica é a <b>composição</b>: quem
 * chama, em que ordem, e como o resultado vira motivo. Isso vive no
 * {@link ArchbaseAccessEvaluator}.
 */
public interface RestrictionEvaluator {

    RestrictionKind kind();

    RestrictionResult evaluate(AccessSubject subject, Restriction restriction, AccessRequirement requirement);

    /**
     * O que a tranca respondeu.
     *
     * @param passed   se o acesso segue adiante
     * @param waived   passou por isenção de administrador, e não por satisfazer a exigência
     * @param detail   explicação legível — o que foi comparado com o quê
     */
    record RestrictionResult(boolean passed, boolean waived, String reasonCode, String detail) {

        public static RestrictionResult passed(String detail) {
            return new RestrictionResult(true, false, null, detail);
        }

        public static RestrictionResult waived(String detail) {
            return new RestrictionResult(true, true, AccessReasonCodes.RESTRICTION_WAIVED_ADMINISTRATOR, detail);
        }

        public static RestrictionResult denied(String reasonCode, String detail) {
            return new RestrictionResult(false, false, reasonCode, detail);
        }
    }
}
