package br.com.archbase.ddd.domain.contracts;


/**
 * Um {@QueryService} possui apenas a responsabilidade de executar consultas ao repositório deentidades e objetos
 * de domínio. Poderá ser usado dentro do padrão de arquitetura CQRS para executar as consultas.
 *
 * @param <T>  Tipo de entidade
 * @param <ID> Tipo de ID da entidade
 * @author edsonmartins
 */
public interface QueryService<T extends AggregateRoot<T, ID>, ID extends Identifier, N extends Number & Comparable<N>> extends Service<T, ID, N> {

}
