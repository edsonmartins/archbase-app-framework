package br.com.archbase.hypersistence.generator;

import io.hypersistence.utils.hibernate.id.Tsid;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para geração de IDs usando TSID (Time-Sorted Unique Identifier).
 * <p>
 * TSID é um formato de identificador que combina um timestamp com um componente
 * aleatório, resultando em IDs que são:
 * </p>
 * <ul>
 *   <li>Ordenáveis por tempo de criação</li>
 *   <li>Únicos globalmente</li>
 *   <li>Mais compactos que UUIDs quando representados como string</li>
 * </ul>
 *
 * <h3>Formato TSID:</h3>
 * <p>
 * Um TSID de 64 bits é composto por:
 * </p>
 * <ul>
 *   <li>42 bits para o timestamp (milissegundos desde epoch customizável)</li>
 *   <li>22 bits para o componente aleatório/node</li>
 * </ul>
 *
 * <h3>Exemplo de uso:</h3>
 * <pre>{@code
 * import io.hypersistence.utils.hibernate.id.Tsid;
 *
 * @Entity
 * public class OrderEntity {
 *
 *     @Id
 *     @Tsid
 *     private Long id;
 *
 *     // ou como String
 *     @Id
 *     @Tsid
 *     private String id;
 * }
 * }</pre>
 *
 * <h3>Vantagens sobre UUID:</h3>
 * <ul>
 *   <li>Ordenável cronologicamente (útil para índices B-tree)</li>
 *   <li>Menor tamanho de armazenamento (8 bytes vs 16 bytes)</li>
 *   <li>Representação string mais curta (13 caracteres vs 36)</li>
 *   <li>Melhor performance em índices clustered</li>
 * </ul>
 *
 * <h3>Quando usar:</h3>
 * <ul>
 *   <li>Quando a ordem de criação é importante para queries</li>
 *   <li>Quando o espaço de armazenamento é uma preocupação</li>
 *   <li>Quando você precisa de IDs gerados no cliente (distributed)</li>
 * </ul>
 *
 * <p>
 * Esta é uma anotação de documentação que aponta para {@link Tsid} do Hypersistence Utils.
 * Use a anotação original {@code @Tsid} do pacote {@code io.hypersistence.utils.hibernate.id}.
 * </p>
 *
 * @author Archbase Team
 * @since 2.1.0
 * @see io.hypersistence.utils.hibernate.id.Tsid
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ArchbaseTsidGenerator {
    // Esta é uma anotação de documentação.
    // Use @Tsid do io.hypersistence.utils.hibernate.id para geração de TSID.
}
