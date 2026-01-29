package br.com.archbase.hypersistence.util;

import io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator;

/**
 * Utilitário para validação de contagem de statements SQL em testes.
 * <p>
 * Esta classe encapsula o {@link SQLStatementCountValidator} do Hypersistence Utils
 * para facilitar a detecção de problemas de N+1 queries em testes.
 * </p>
 *
 * <h3>Exemplo de uso:</h3>
 * <pre>{@code
 * @Test
 * void shouldNotHaveNPlusOneQueries() {
 *     SQLStatementCountAssertions.reset();
 *
 *     List<Order> orders = orderRepository.findAllWithItems();
 *
 *     // Verifica que apenas 1 SELECT foi executado
 *     SQLStatementCountAssertions.assertSelectCount(1);
 * }
 * }</pre>
 *
 * <h3>Configuração necessária:</h3>
 * <p>
 * Para que a contagem funcione, é necessário configurar o datasource-proxy.
 * Adicione ao application-test.properties:
 * </p>
 * <pre>
 * # Habilitar proxy do datasource para contagem de queries
 * spring.datasource.driver-class-name=net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
 * </pre>
 *
 * @author Archbase Team
 * @since 2.1.0
 * @see io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator
 */
public final class SQLStatementCountAssertions {

    private SQLStatementCountAssertions() {
        // Utility class
    }

    /**
     * Reseta os contadores de statements SQL.
     * <p>
     * Deve ser chamado no início de cada teste para garantir
     * uma contagem limpa.
     * </p>
     */
    public static void reset() {
        SQLStatementCountValidator.reset();
    }

    /**
     * Verifica se o número de SELECTs executados corresponde ao esperado.
     *
     * @param expected Número esperado de SELECTs
     * @throws AssertionError se o número de SELECTs for diferente do esperado
     */
    public static void assertSelectCount(int expected) {
        SQLStatementCountValidator.assertSelectCount(expected);
    }

    /**
     * Verifica se o número de INSERTs executados corresponde ao esperado.
     *
     * @param expected Número esperado de INSERTs
     * @throws AssertionError se o número de INSERTs for diferente do esperado
     */
    public static void assertInsertCount(int expected) {
        SQLStatementCountValidator.assertInsertCount(expected);
    }

    /**
     * Verifica se o número de UPDATEs executados corresponde ao esperado.
     *
     * @param expected Número esperado de UPDATEs
     * @throws AssertionError se o número de UPDATEs for diferente do esperado
     */
    public static void assertUpdateCount(int expected) {
        SQLStatementCountValidator.assertUpdateCount(expected);
    }

    /**
     * Verifica se o número de DELETEs executados corresponde ao esperado.
     *
     * @param expected Número esperado de DELETEs
     * @throws AssertionError se o número de DELETEs for diferente do esperado
     */
    public static void assertDeleteCount(int expected) {
        SQLStatementCountValidator.assertDeleteCount(expected);
    }

    /**
     * Verifica se o número total de statements SQL executados corresponde ao esperado.
     *
     * @param expected Número esperado de statements totais
     * @throws AssertionError se o número total for diferente do esperado
     */
    public static void assertTotalCount(int expected) {
        SQLStatementCountValidator.assertTotalCount(expected);
    }

    /**
     * Verifica se nenhum SELECT foi executado.
     * <p>
     * Útil para verificar que uma operação não fez queries desnecessárias.
     * </p>
     *
     * @throws AssertionError se algum SELECT foi executado
     */
    public static void assertNoSelect() {
        assertSelectCount(0);
    }

    /**
     * Verifica se nenhum INSERT foi executado.
     *
     * @throws AssertionError se algum INSERT foi executado
     */
    public static void assertNoInsert() {
        assertInsertCount(0);
    }

    /**
     * Verifica se nenhum UPDATE foi executado.
     *
     * @throws AssertionError se algum UPDATE foi executado
     */
    public static void assertNoUpdate() {
        assertUpdateCount(0);
    }

    /**
     * Verifica se nenhum DELETE foi executado.
     *
     * @throws AssertionError se algum DELETE foi executado
     */
    public static void assertNoDelete() {
        assertDeleteCount(0);
    }
}
