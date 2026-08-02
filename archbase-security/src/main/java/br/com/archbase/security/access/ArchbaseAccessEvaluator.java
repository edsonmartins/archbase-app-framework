package br.com.archbase.security.access;

/**
 * O único lugar onde uma decisão de acesso é tomada no Archbase.
 *
 * <p><b>As anotações param de decidir. Elas passam a declarar.</b> {@code @HasPermission},
 * {@code @RequireRole}, {@code @RequireProfile} e {@code @RequirePersona} montam um
 * {@link AccessRequirement} e delegam; quem avalia é esta interface, com uma regra só e uma cadeia
 * de motivos só.
 *
 * <p>Ver {@code MODELO_CORE_AUTORIZACAO.md} para o desenho, e {@link Gate} para a hierarquia dos
 * portões.
 */
public interface ArchbaseAccessEvaluator {

    /**
     * Decide se o sujeito pode exercer a capacidade pedida.
     *
     * <p>Nunca lança por dado ausente ou inconsistente: a resposta a "não consegui avaliar" é uma
     * negação <b>com motivo</b>, não uma exceção que o chamador transforma em 403 anônimo.
     */
    AccessDecision decide(AccessSubject subject, AccessRequirement requirement);
}
