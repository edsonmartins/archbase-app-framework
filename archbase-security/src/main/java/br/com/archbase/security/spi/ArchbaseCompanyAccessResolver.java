package br.com.archbase.security.spi;

import br.com.archbase.security.persistence.UserEntity;

/**
 * Diz se um usuário pode atuar numa empresa, para conferir o {@code X-COMPANY-ID} da requisição.
 *
 * <p><b>Por que é um SPI.</b> O {@code X-COMPANY-ID} alimenta o {@code companyFilter} do Hibernate e o escopo de
 * empresa do {@code @HasPermission}, mas o vínculo usuário–empresa não existe no modelo do Archbase: mora no domínio
 * da aplicação (filial, loja, unidade). O framework precisa perguntar.
 *
 * <p>Só é consultado com {@code archbase.security.company.validation} em {@code warn} ou {@code enforce}. Com
 * {@code enforce} e sem nenhum bean desta interface, a aplicação não sobe.
 *
 * <pre>{@code
 * @Component
 * public class EmpresasDoUsuario implements ArchbaseCompanyAccessResolver {
 *     private final VinculoEmpresaRepository vinculos;
 *
 *     @Override
 *     public boolean canAccess(UserEntity user, String companyId) {
 *         return vinculos.existsByUsuarioIdAndEmpresaId(user.getId(), companyId);
 *     }
 * }
 * }</pre>
 *
 * <p>Roda em toda requisição autenticada que traga {@code X-COMPANY-ID}, já com o tenant no contexto: mantenha a
 * consulta barata (índice, cache de curta duração).
 *
 * @since 3.5.0
 */
public interface ArchbaseCompanyAccessResolver {

    /**
     * @param user      usuário autenticado, no tenant da requisição
     * @param companyId valor do {@code X-COMPANY-ID}, nunca vazio
     * @return {@code true} se o usuário pode atuar nessa empresa. Exceção é tratada como {@code false}.
     */
    boolean canAccess(UserEntity user, String companyId);
}
