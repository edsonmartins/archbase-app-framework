/**
 * Tipos Hibernate customizados disponíveis através do Hypersistence Utils.
 *
 * <h2>Tipos JSON</h2>
 * <p>
 * Use {@code @Type(JsonType.class)} para mapear campos para colunas JSON.
 * Funciona com PostgreSQL (jsonb), MySQL (json), Oracle e H2.
 * </p>
 *
 * <h3>Exemplos de uso com JSON:</h3>
 * <pre>{@code
 * import io.hypersistence.utils.hibernate.type.json.JsonType;
 * import org.hibernate.annotations.Type;
 *
 * @Entity
 * public class ProductEntity extends TenantPersistenceEntityBase<ProductEntity, String> {
 *
 *     // Map<String, Object> - Armazena qualquer estrutura JSON
 *     @Type(JsonType.class)
 *     @Column(columnDefinition = "jsonb")
 *     private Map<String, Object> metadata;
 *
 *     // List<String> - Armazena array JSON
 *     @Type(JsonType.class)
 *     @Column(columnDefinition = "jsonb")
 *     private List<String> tags;
 *
 *     // POJO customizado - Armazena objeto serializado
 *     @Type(JsonType.class)
 *     @Column(columnDefinition = "jsonb")
 *     private ProductDetails details;
 *
 *     // Jackson JsonNode - Para JSON dinâmico
 *     @Type(JsonType.class)
 *     @Column(columnDefinition = "jsonb")
 *     private JsonNode configuration;
 * }
 * }</pre>
 *
 * <h2>Tipos Array (PostgreSQL)</h2>
 * <p>
 * Suporte a arrays nativos do PostgreSQL como text[], int[], uuid[], etc.
 * </p>
 *
 * <h3>Exemplos de uso com Arrays:</h3>
 * <pre>{@code
 * import io.hypersistence.utils.hibernate.type.array.StringArrayType;
 * import io.hypersistence.utils.hibernate.type.array.IntArrayType;
 * import io.hypersistence.utils.hibernate.type.array.UUIDArrayType;
 * import io.hypersistence.utils.hibernate.type.array.ListArrayType;
 *
 * @Entity
 * public class EventEntity extends PersistenceEntityBase<EventEntity, String> {
 *
 *     // Array de strings
 *     @Type(StringArrayType.class)
 *     @Column(columnDefinition = "text[]")
 *     private String[] participants;
 *
 *     // Array de inteiros
 *     @Type(IntArrayType.class)
 *     @Column(columnDefinition = "int[]")
 *     private int[] scores;
 *
 *     // Array de UUIDs
 *     @Type(UUIDArrayType.class)
 *     @Column(columnDefinition = "uuid[]")
 *     private UUID[] relatedIds;
 *
 *     // Lista como array
 *     @Type(ListArrayType.class)
 *     @Column(columnDefinition = "text[]")
 *     private List<String> categories;
 * }
 * }</pre>
 *
 * <h2>Tipos Range (PostgreSQL)</h2>
 * <p>
 * Suporte a tipos range do PostgreSQL como daterange, int4range, etc.
 * </p>
 *
 * <h3>Exemplos de uso com Ranges:</h3>
 * <pre>{@code
 * import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
 * import io.hypersistence.utils.hibernate.type.range.Range;
 *
 * @Entity
 * public class ReservationEntity extends PersistenceEntityBase<ReservationEntity, String> {
 *
 *     // Range de datas
 *     @Type(PostgreSQLRangeType.class)
 *     @Column(columnDefinition = "daterange")
 *     private Range<LocalDate> reservationPeriod;
 *
 *     // Range de inteiros
 *     @Type(PostgreSQLRangeType.class)
 *     @Column(columnDefinition = "int4range")
 *     private Range<Integer> ageRange;
 *
 *     // Range de BigDecimal
 *     @Type(PostgreSQLRangeType.class)
 *     @Column(columnDefinition = "numrange")
 *     private Range<BigDecimal> priceRange;
 * }
 * }</pre>
 *
 * <h2>Tipos PostgreSQL Específicos</h2>
 *
 * <h3>HStore (Map key-value):</h3>
 * <pre>{@code
 * import io.hypersistence.utils.hibernate.type.basic.PostgreSQLHStoreType;
 *
 * @Type(PostgreSQLHStoreType.class)
 * @Column(columnDefinition = "hstore")
 * private Map<String, String> attributes;
 * }</pre>
 *
 * <h3>Inet (Endereços IP):</h3>
 * <pre>{@code
 * import io.hypersistence.utils.hibernate.type.basic.PostgreSQLInetType;
 * import io.hypersistence.utils.hibernate.type.basic.Inet;
 *
 * @Type(PostgreSQLInetType.class)
 * @Column(columnDefinition = "inet")
 * private Inet ipAddress;
 * }</pre>
 *
 * <h2>Notas de Compatibilidade</h2>
 * <ul>
 *   <li>Tipos JSON funcionam com todos os bancos de dados suportados</li>
 *   <li>Tipos Array funcionam apenas com PostgreSQL</li>
 *   <li>Tipos Range funcionam apenas com PostgreSQL</li>
 *   <li>Para testes com H2, use apenas tipos JSON</li>
 * </ul>
 *
 * @author Archbase Team
 * @since 2.1.0
 * @see io.hypersistence.utils.hibernate.type.json.JsonType
 * @see io.hypersistence.utils.hibernate.type.array.StringArrayType
 * @see io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType
 */
package br.com.archbase.hypersistence.types;
