package br.com.archbase.security.access;

/**
 * O tipo de tranca declarada por uma anotação de restrição.
 *
 * <p>Cada tipo tem seu próprio {@link RestrictionEvaluator}, e cada um mantém a ordem de checagem
 * que a anotação correspondente sempre teve — que <b>não é a mesma entre eles</b>. {@code ROLE}
 * verifica a conta desativada antes da isenção de administrador; {@code PROFILE} e {@code PERSONA}
 * verificam depois. Unificar essa ordem mudaria decisão, então ela é preservada por tipo.
 */
public enum RestrictionKind {

    /** {@code @RequireRole} — papéis do domínio da aplicação, resolvidos por SPI. */
    ROLE,

    /** {@code @RequireProfile} — o perfil do usuário, um por usuário. */
    PROFILE,

    /** {@code @RequirePersona} — personas de negócio. */
    PERSONA
}
