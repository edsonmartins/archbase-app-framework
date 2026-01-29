package br.com.archbase.hypersistence.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Interface de repositório com métodos otimizados do Hypersistence Utils.
 * <p>
 * Esta interface fornece métodos mais eficientes para operações de persistência
 * em comparação com o método {@code save()} padrão do Spring Data JPA.
 * </p>
 *
 * <h3>Quando usar cada método:</h3>
 * <ul>
 *   <li><b>persist()</b> - Para entidades NOVAS que nunca foram persistidas.
 *       Não faz SELECT antes do INSERT, sendo mais eficiente.</li>
 *   <li><b>merge()</b> - Para entidades EXISTENTES que precisam ser atualizadas.
 *       Faz merge do estado da entidade.</li>
 *   <li><b>update()</b> - Para atualizações em lote de entidades existentes.
 *       Otimizado para múltiplas atualizações.</li>
 * </ul>
 *
 * <h3>Diferença entre save() e persist():</h3>
 * <p>
 * O método {@code save()} do Spring Data JPA faz um SELECT antes do INSERT
 * para verificar se a entidade existe. O método {@code persist()} assume
 * que a entidade é nova e faz apenas o INSERT, sendo mais eficiente.
 * </p>
 *
 * <h3>Exemplo de uso:</h3>
 * <pre>{@code
 * public interface ProductRepository
 *     extends ArchbaseJpaRepository<ProductEntity, String, Long>,
 *             ArchbaseHypersistenceRepository<ProductEntity, String> {
 * }
 *
 * // No serviço
 * @Service
 * public class ProductService {
 *     public void createProducts(List<ProductEntity> products) {
 *         // Mais eficiente que save() para entidades novas
 *         products.forEach(repository::persist);
 *     }
 * }
 * }</pre>
 *
 * @param <T>  Tipo da entidade
 * @param <ID> Tipo do identificador da entidade
 * @author Archbase Team
 * @since 2.1.0
 */
@NoRepositoryBean
public interface ArchbaseHypersistenceRepository<T, ID> extends Repository<T, ID> {

    /**
     * Persiste uma nova entidade no banco de dados.
     * <p>
     * Este método é mais eficiente que {@code save()} para entidades novas,
     * pois não faz SELECT antes do INSERT.
     * </p>
     * <p>
     * <b>Importante:</b> Use apenas para entidades NOVAS. Se a entidade
     * já existir no banco, uma exceção será lançada.
     * </p>
     *
     * @param entity A entidade a ser persistida (não pode ser nula)
     * @param <S>    Subtipo da entidade
     * @return A entidade persistida
     */
    <S extends T> S persist(S entity);

    /**
     * Persiste uma nova entidade e faz flush imediato ao banco de dados.
     * <p>
     * Equivalente a chamar {@code persist()} seguido de {@code flush()}.
     * Útil quando você precisa garantir que a entidade foi persistida
     * imediatamente.
     * </p>
     *
     * @param entity A entidade a ser persistida (não pode ser nula)
     * @param <S>    Subtipo da entidade
     * @return A entidade persistida
     */
    <S extends T> S persistAndFlush(S entity);

    /**
     * Persiste múltiplas entidades novas.
     * <p>
     * Otimizado para operações em lote. Mais eficiente que chamar
     * {@code persist()} individualmente para cada entidade.
     * </p>
     *
     * @param entities As entidades a serem persistidas
     * @param <S>      Subtipo da entidade
     * @return Lista das entidades persistidas
     */
    <S extends T> List<S> persistAll(Iterable<S> entities);

    /**
     * Persiste múltiplas entidades e faz flush imediato.
     *
     * @param entities As entidades a serem persistidas
     * @param <S>      Subtipo da entidade
     * @return Lista das entidades persistidas
     */
    <S extends T> List<S> persistAllAndFlush(Iterable<S> entities);

    /**
     * Faz merge de uma entidade existente (detached) com o contexto de persistência.
     * <p>
     * Use este método quando tiver uma entidade que foi modificada fora do
     * contexto de persistência e precisa ser sincronizada com o banco.
     * </p>
     *
     * @param entity A entidade a ser merged (não pode ser nula)
     * @param <S>    Subtipo da entidade
     * @return A entidade merged
     */
    <S extends T> S merge(S entity);

    /**
     * Faz merge de uma entidade e flush imediato.
     *
     * @param entity A entidade a ser merged (não pode ser nula)
     * @param <S>    Subtipo da entidade
     * @return A entidade merged
     */
    <S extends T> S mergeAndFlush(S entity);

    /**
     * Faz merge de múltiplas entidades.
     *
     * @param entities As entidades a serem merged
     * @param <S>      Subtipo da entidade
     * @return Lista das entidades merged
     */
    <S extends T> List<S> mergeAll(Iterable<S> entities);

    /**
     * Faz merge de múltiplas entidades e flush imediato.
     *
     * @param entities As entidades a serem merged
     * @param <S>      Subtipo da entidade
     * @return Lista das entidades merged
     */
    <S extends T> List<S> mergeAllAndFlush(Iterable<S> entities);

    /**
     * Atualiza uma entidade existente no banco de dados.
     * <p>
     * Este método é otimizado para atualizações e não faz SELECT prévio
     * para verificar a existência da entidade.
     * </p>
     *
     * @param entity A entidade a ser atualizada (não pode ser nula)
     * @param <S>    Subtipo da entidade
     * @return A entidade atualizada
     */
    <S extends T> S update(S entity);

    /**
     * Atualiza uma entidade e faz flush imediato.
     *
     * @param entity A entidade a ser atualizada (não pode ser nula)
     * @param <S>    Subtipo da entidade
     * @return A entidade atualizada
     */
    <S extends T> S updateAndFlush(S entity);

    /**
     * Atualiza múltiplas entidades.
     *
     * @param entities As entidades a serem atualizadas
     * @param <S>      Subtipo da entidade
     * @return Lista das entidades atualizadas
     */
    <S extends T> List<S> updateAll(Iterable<S> entities);

    /**
     * Atualiza múltiplas entidades e faz flush imediato.
     *
     * @param entities As entidades a serem atualizadas
     * @param <S>      Subtipo da entidade
     * @return Lista das entidades atualizadas
     */
    <S extends T> List<S> updateAllAndFlush(Iterable<S> entities);
}
