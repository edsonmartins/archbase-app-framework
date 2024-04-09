package br.com.archbase.ddd.domain.contracts;


/**
 * Um {@CommandService} possui apenas a responsabilidade de executar comandos em entidades e objetos
 * de domínio. Poderá ser usado dentro do padrão de arquitetura CQRS para executar os comandos.
 *
 * @param <T>  Tipo de entidade
 * @param <ID> Tipo de ID da entidade
 * @author edsonmartins
 */
public interface CommandService<T extends AggregateRoot<T, ID>, ID extends Identifier, N extends Number & Comparable<N>> extends Service<T, ID, N> {

}
