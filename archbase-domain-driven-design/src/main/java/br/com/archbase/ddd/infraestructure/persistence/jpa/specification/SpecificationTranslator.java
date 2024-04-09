package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Capaz de traduzir instâncias específicas de domínio {@link ArchbaseSpecification} em uma consulta {@link Predicate}.
 * Traduzir especificações em critérios de consulta nos permite construir e executar consultas de alto desempenho,
 * que ainda capturam a lógica de nossa especificação relacionada ao domínio.
 *
 * @author edsonmartins
 */
public interface SpecificationTranslator {

    /**
     * Traduzir algumas {@link ArchbaseSpecification} em um novo {@link Predicate}, aplicando os mesmos critérios de seleção
     * como nosso archbaseSpecification fornecido. Os predicados retornados só podem ser usados na consulta de critérios fornecida.
     *
     * @param <T>                   tipo das entidades descritas em nosso archbaseSpecification
     * @param archbaseSpecification descreve a lógica de negócios que devemos fornecer em nosso predicado
     * @param root                  caminho para a raiz da nossa consulta, naturalmente, esse caminho deve ser criado pela consulta
     * @param cq                    a consulta que conterá nosso predicado retornado pode ser usada para fazer subconsultas
     * @param cb                    instância de construtor usada para construir novos predicados
     * @return novo predicado que impõe nossa lógica archbaseSpecification
     */
    <T> Predicate translateToPredicate(ArchbaseSpecification<T> archbaseSpecification, Root<T> root, CriteriaQuery<?> cq, CriteriaBuilder cb);

    /**
     * Registrar um conversor, permitindo que ele seja utilizado durante a conversão de uma especificação. Sempre que uma
     * especificação está sendo convertida, certifique-se de que alguma instância do conversor correspondente foi registrada.
     *
     * @param converter nova instância do conversor que deve ser registrada
     * @return esta instância do tradutor, usada para habilitar o encadeamento
     */
    SpecificationTranslator registerConverter(SpecificationConverter<?, ?> converter);

}
