package br.com.archbase.query.rls.support;

import br.com.archbase.query.rls.support.exceptions.ArchbaseRowLevelSecurityFilterException;
import org.springframework.security.core.GrantedAuthority;

import java.util.Iterator;


/**
 * Esta interface possibilita criar regras/predicados para filtrar as linhas de uma seleção de dados.
 * <p>
 * RSL - Row Security Level.
 * <p>
 * Os predicados de filtro RLS são funcionalmente equivalentes a acrescentar uma cláusula WHERE .
 * O predicado pode ser tão sofisticado como ditam as práticas comerciais, ou a cláusula pode ser tão simples
 * quanto WHERE TenantId = 42.
 * Em termos mais formais, o RLS introduz o controle de acesso baseado em predicado. Ele apresenta uma avaliação
 * centralizada, flexível e baseada em predicado. O predicado pode ser baseado em metadados ou em qualquer outro
 * critério que o administrador determine, como apropriado. O predicado é usado como critério para determinar se o
 * usuário tem acesso apropriado aos dados com base nos atributos de usuário. O controle de acesso baseado em rótulo
 * pode ser implementado usando o controle de acesso baseado em predicado.
 * <p>
 * <p>
 * Casos de uso
 * ------------
 * <p>
 * Estes são exemplos de design de como RLS pode ser usado:
 * <ul>
 *     <li>Um hospital pode criar uma diretiva de segurança que permite a enfermeiras exibir linhas de dados somente para seus pacientes.</li>
 *     <li>Um banco pode criar uma política para restringir o acesso às linhas de dados financeiros com base no cargo ou divisão de negócios
 *         de um funcionário na empresa.
 *     </li>
 *     <li>Um aplicativo multilocatário pode criar uma política para impor uma separação lógica entre as linhas de dados de cada locatário
 *         e as linhas referentes a todos os outros locatários. Eficiência é obtida pelo armazenamento de dados para vários locatários em uma
 *         única tabela. Cada locatário pode ver somente as linhas com seus próprios dados.
 *      </li>
 * </ul>
 *
 * @author edsonmartins
 */
@SuppressWarnings({"rawtypes"})
public interface RowLevelSecurityFilter {

//    public void filter(
//            Root from,
//            CriteriaQueryImpl criteriaQuery,
//            Iterator<? extends GrantedAuthority> rolesItr)
//            throws ArchbaseRowLevelSecurityFilterException;

    public void filter(
            String criteriaQuery,
            Iterator<? extends GrantedAuthority> rolesItr)
            throws ArchbaseRowLevelSecurityFilterException;
}
