package br.com.archbase.security.spi;

import br.com.archbase.security.persistence.UserEntity;

import java.util.Set;

/**
 * Fonte das roles de negócio de um usuário, para uso pela anotação
 * {@link br.com.archbase.security.annotations.RequireRole}.
 *
 * <p><b>Por que é um SPI e não uma tabela.</b> {@code @RequireRole} existe justamente para roles
 * que <i>não</i> são o {@code Profile}/{@code Group} do Archbase — são do domínio da aplicação
 * ("STORE_ADMIN", "DRIVER", "PLATFORM_ADMIN") e moram no modelo dela. O framework não tem como
 * descobri-las sozinho; precisa perguntar.
 *
 * <p><b>Sem uma implementação, {@code @RequireRole} não valida nada.</b> Declare um bean desta
 * interface na aplicação:
 *
 * <pre>{@code
 * @Component
 * public class MinhasRoles implements ArchbaseRoleResolver {
 *     private final LojaRepository lojas;
 *
 *     @Override
 *     public Set<String> resolveRoles(UserEntity user) {
 *         return lojas.findRolesDoUsuario(user.getId());
 *     }
 *
 *     @Override
 *     public boolean isOwner(UserEntity user) {
 *         return lojas.isProprietario(user.getId());
 *     }
 * }
 * }</pre>
 *
 * @see br.com.archbase.security.config.RoleAuthorizationManager
 */
public interface ArchbaseRoleResolver {

    /**
     * Roles de negócio do usuário. Nunca {@code null} — devolva {@link Set#of()} quando não houver
     * nenhuma, para que a comparação resulte em negação em vez de erro.
     */
    Set<String> resolveRoles(UserEntity user);

    /**
     * Suporte a {@code @RequireRole(ownerOnly = true)}: verdadeiro se o usuário é proprietário e
     * não apenas alguém vinculado (funcionário, colaborador).
     *
     * <p>O padrão é {@code false} — nega — porque "não sei responder" e "é proprietário" não podem
     * dar no mesmo resultado num controle de acesso.
     */
    default boolean isOwner(UserEntity user) {
        return false;
    }
}
