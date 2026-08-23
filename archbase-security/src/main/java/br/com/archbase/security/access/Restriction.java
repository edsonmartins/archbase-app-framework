package br.com.archbase.security.access;

import java.util.List;

/**
 * Uma tranca declarada sobre o método — o que {@code @RequireRole}, {@code @RequireProfile} e
 * {@code @RequirePersona} passam a produzir em vez de decidir por conta própria.
 *
 * <p><b>Restrição só nega.</b> Passar por ela não abre nada: quem concede é o catálogo, no portão
 * {@link Gate#GRANT}. A única exceção é {@link #allowSystemAdmin()}, que <b>isenta</b> o
 * administrador desta tranca específica — comportamento que as três anotações têm desde sempre, com
 * padrão {@code true}, e que o core preserva como isenção local e explícita, nunca como desvio
 * global.
 *
 * @param required            valores exigidos pela anotação
 * @param requireAll          exige todos, e não apenas um
 * @param allowSystemAdmin    isenta o administrador desta tranca
 * @param requireActiveUser   exige conta ativa ({@code PROFILE} e {@code PERSONA})
 * @param requirePlatformAdmin exige {@code isAdministrator} ({@code ROLE})
 * @param ownerOnly           exige propriedade, respondida pelo SPI ({@code ROLE})
 * @param context             contexto de negócio declarado ({@code PERSONA})
 * @param message             mensagem da anotação, para o 403
 */
public record Restriction(
        RestrictionKind kind,
        List<String> required,
        boolean requireAll,
        boolean allowSystemAdmin,
        boolean requireActiveUser,
        boolean requirePlatformAdmin,
        boolean ownerOnly,
        String context,
        String message) {

    public Restriction {
        required = required == null ? List.of() : List.copyOf(required);
    }

    public static Restriction role(List<String> required, boolean requireAll, boolean allowSystemAdmin,
                                   boolean requirePlatformAdmin, boolean ownerOnly,
                                   String context, String message) {
        return new Restriction(RestrictionKind.ROLE, required, requireAll, allowSystemAdmin,
                false, requirePlatformAdmin, ownerOnly, context, message);
    }

    public static Restriction profile(List<String> required, boolean requireAll, boolean allowSystemAdmin,
                                      boolean requireActiveUser, String message) {
        return new Restriction(RestrictionKind.PROFILE, required, requireAll, allowSystemAdmin,
                requireActiveUser, false, false, null, message);
    }

    public static Restriction persona(List<String> required, boolean requireAll, boolean allowSystemAdmin,
                                      boolean requireActiveUser, boolean ownerOnly,
                                      String context, String message) {
        return new Restriction(RestrictionKind.PERSONA, required, requireAll, allowSystemAdmin,
                requireActiveUser, false, ownerOnly, context, message);
    }
}
