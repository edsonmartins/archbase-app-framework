package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * ArchbaseSpecification capaz de se traduzir em um Java Persistence API específico
 * {@link Predicate}. O predicado retornado impõe um critério de seleção em apenas
 * entidades que atendem às regras de negócios da especificação.
 *
 * @param <T> tipo de entidade representada em nossa lógica de especificação
 * @author edsonmartins
 */
public interface JpaArchbaseSpecification<T> extends ArchbaseSpecification<T> {

    /**
     * Construa um {@link Predicate} que aplique os critérios desta especificação.
     *
     * @param root de raiz para a raiz de nossa consulta
     * @param cq   query que conterá nossos critérios de especificação
     * @param cb   Critérios construtor, usado para construir predicados
     * @return o predicado convertido
     */
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> cq, CriteriaBuilder cb);

}
