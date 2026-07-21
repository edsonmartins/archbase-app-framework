package br.com.archbase.security.password;

import java.time.LocalDateTime;

/**
 * Política de expiração periódica de senha.
 *
 * <p>Mantém a configuração em estado estático porque quem consulta a política é a
 * {@code UserEntity} — um objeto gerenciado pelo JPA, sem injeção de dependência — no
 * momento em que o Spring Security avalia {@code UserDetails#isCredentialsNonExpired()}.
 *
 * <p>A política é <b>desligada por padrão</b> ({@code expirationDays = 0}): sem
 * configuração explícita, nenhuma senha expira por tempo. Habilite com:
 *
 * <pre>archbase.security.password.expiration-days=90</pre>
 *
 * @see br.com.archbase.security.config.ArchbasePasswordPolicyConfiguration
 */
public final class ArchbasePasswordPolicy {

    /** Valor de {@code expirationDays} que desliga a expiração por tempo. */
    public static final int NO_EXPIRATION = 0;

    private static volatile int expirationDays = NO_EXPIRATION;

    private ArchbasePasswordPolicy() {
    }

    public static int getExpirationDays() {
        return expirationDays;
    }

    /**
     * @param days quantidade de dias de validade da senha; {@code <= 0} desliga a expiração.
     */
    public static void setExpirationDays(int days) {
        expirationDays = Math.max(days, NO_EXPIRATION);
    }

    public static boolean isExpirationEnabled() {
        return expirationDays > NO_EXPIRATION;
    }

    /**
     * Indica se uma senha alterada em {@code passwordChangedAt} já ultrapassou o prazo de validade.
     *
     * @param passwordChangedAt data da última troca de senha. Quando {@code null} (usuário legado,
     *                          anterior à coluna de histórico), a senha <b>não</b> é considerada
     *                          expirada — do contrário, ligar a política derrubaria o login de toda
     *                          a base de uma vez.
     */
    public static boolean isExpired(LocalDateTime passwordChangedAt) {
        if (!isExpirationEnabled() || passwordChangedAt == null) {
            return false;
        }
        return passwordChangedAt.plusDays(expirationDays).isBefore(LocalDateTime.now());
    }
}
