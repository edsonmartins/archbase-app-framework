package br.com.archbase.ddd.domain.contracts;


/**
 * Os Serviços de Domínio implementam a lógica de negócios a partir da definição de um expert de domínio.
 * Trabalham com diversos fluxos de diversas entidades e agregações, utilizam os repositórios como interface
 * de acesso aos dados e consomem recursos da camada de infraestrutura, como: enviar email, disparar eventos,
 * entre outros.
 *
 * @param <T>  Tipo de entidade
 * @param <ID> Tipo de ID da entidade
 * @author edsonmartins
 */
public interface Service<T extends AggregateRoot<T, ID>, ID extends Identifier, N extends Number & Comparable<N>> {

    public Repository<T, ID, N> getRepository();

}
