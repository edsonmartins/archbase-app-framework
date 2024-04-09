package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Especifica a lógica de conversão de um {@link ArchbaseSpecification} em {@link Predicate}.
 *
 * @param <T> tipo de candidatos, verificado na especificação
 * @param <S> tipo de especificação sendo convertida
 * @author edsonmartins
 */
public interface SpecificationConverter<S extends ArchbaseSpecification<T>, T> {

    /**
     * Converta algumas {@link ArchbaseSpecification} em uma Java Persistence API {@link Predicate}.
     * O predicado retornado impõe um critério de seleção em apenas entidades que satisfaçam
     * as regras de negócios da especificação.
     *
     * @param specification nossa especificação de negócios sendo convertida
     * @param root          caminho para a raiz de nossa consulta
     * @param cq            consulta que conterá nossos critérios de especificação
     * @param cb            construtor de critérios, usado para construir predicados
     * @return nosso predicado construído, obrigando apenas entidades correspondentes a serem selecionadas
     */
    Predicate convertToPredicate(S specification, Root<T> root, CriteriaQuery<?> cq, CriteriaBuilder cb);

}
