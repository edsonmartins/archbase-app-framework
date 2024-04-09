package br.com.archbase.ddd.domain.contracts;

/**
 * Identifica uma raiz agregada, ou seja, a entidade raiz de um agregado. Um agregado forma um cluster de regras consistentes
 * geralmente formado em torno de um conjunto de entidades, definindo invariantes com base nas propriedades do agregado que devem
 * ser encontrado antes e depois das operações nele. Os agregados geralmente se referem a outros agregados por seu identificador.
 * Referências a agregados internos devem ser evitadas e, pelo menos, não consideradas fortemente consistentes (ou seja, uma referência
 * retido pode ter desaparecido ou se tornar inválido a qualquer momento). Eles também atuam como escopo de consistência,
 * ou seja, as mudanças em um único agregado devem ser fortemente consistentes, enquanto as mudanças em vários outros devem
 * ser apenas consistência eventual.
 *
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Aggregates</a>
 */
public interface AggregateRoot<T extends AggregateRoot<T, ID>, ID extends Identifier> extends Entity<T, ID> {

}
