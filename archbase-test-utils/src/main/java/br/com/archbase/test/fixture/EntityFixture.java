package br.com.archbase.test.fixture;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Utilitário para criar fixtures de entidades para testes.
 * Mantém um registro de templates de entidades que podem ser reutilizados.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * // Registrar um template
 * EntityFixture.register("client-valid", () -> {
 *     ClientEntity client = new ClientEntity();
 *     client.setName("Cliente Teste");
 *     client.setEmail("cliente@teste.com");
 *     return client;
 * });
 *
 * // Usar o template
 * ClientEntity client = EntityFixture.create("client-valid");
 * }
 * </pre>
 */
public class EntityFixture {

    private static final Map<String, Supplier<?>> fixtures = new HashMap<>();

    private EntityFixture() {
        // Utilitário estático
    }

    /**
     * Registra um novo template de fixture usando um factory method.
     *
     * @param name    Nome identificador do fixture
     * @param factory Factory method que cria a instância
     * @param <T>     Tipo da entidade
     */
    public static <T> void register(String name, Supplier<T> factory) {
        fixtures.put(name, factory);
    }

    /**
     * Cria uma nova instância usando o template registrado.
     *
     * @param name Nome do template
     * @param <T>  Tipo da entidade
     * @return Nova instância da entidade
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(String name) {
        Supplier<?> factory = fixtures.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Fixture '" + name + "' não encontrado. " +
                    "Registre o fixture antes de usar.");
        }
        return (T) factory.get();
    }

    /**
     * Remove um template registrado.
     *
     * @param name Nome do template
     */
    public static void unregister(String name) {
        fixtures.remove(name);
    }

    /**
     * Limpa todos os templates registrados.
     */
    public static void clear() {
        fixtures.clear();
    }

    /**
     * Verifica se um template está registrado.
     *
     * @param name Nome do template
     * @return true se registrado, false caso contrário
     */
    public static boolean isRegistered(String name) {
        return fixtures.containsKey(name);
    }
}
