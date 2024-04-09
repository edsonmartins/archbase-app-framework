package br.com.archbase.query.rsql.jpa;

import br.com.archbase.query.rsql.common.RSQLCommonSupport;
import br.com.archbase.query.rsql.common.RSQLCustomPredicate;
import br.com.archbase.query.rsql.common.RSQLOperators;
import br.com.archbase.query.rsql.parser.RSQLParser;
import br.com.archbase.query.rsql.parser.ast.ComparisonOperator;
import br.com.archbase.query.rsql.parser.ast.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Order;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author edsonmartins
 */
@Slf4j
@SuppressWarnings({"rawtypes", "serial"})
public class ArchbaseRSQLJPASupport extends RSQLCommonSupport {

    public ArchbaseRSQLJPASupport() {
        super();
    }

    public ArchbaseRSQLJPASupport(Map<String, EntityManager> entityManagerMap) {
        super(entityManagerMap);
    }

    public static <T> Specification<T> rsql(final String rsqlQuery) {
        return toSpecification(rsqlQuery, false, null);
    }

    public static <T> Specification<T> rsql(final String rsqlQuery, final boolean distinct) {
        return toSpecification(rsqlQuery, distinct, null);
    }

    public static <T> Specification<T> rsql(final String rsqlQuery, final Map<String, String> propertyPathMapper) {
        return toSpecification(rsqlQuery, false, propertyPathMapper);
    }

    public static <T> Specification<T> rsql(final String rsqlQuery, final boolean distinct, final Map<String, String> propertyPathMapper) {
        return toSpecification(rsqlQuery, distinct, propertyPathMapper);
    }

    public static <T> Specification<T> rsql(final String rsqlQuery, final List<RSQLCustomPredicate<?>> customPredicates) {
        return toSpecification(rsqlQuery, customPredicates);
    }

    public static <T> Specification<T> toSpecification(final String rsqlQuery) {
        return toSpecification(rsqlQuery, false, null);
    }

    public static <T> Specification<T> toSpecification(final String rsqlQuery, final Map<String, String> propertyPathMapper) {
        return toSpecification(rsqlQuery, false, propertyPathMapper);
    }

    public static <T> Specification<T> toSpecification(final String rsqlQuery, final boolean distinct) {
        return toSpecification(rsqlQuery, distinct, null);
    }

    public static <T> Specification<T> toSpecification(final String rsqlQuery, final boolean distinct, final Map<String, String> propertyPathMapper) {
        return toSpecification(rsqlQuery, distinct, propertyPathMapper, null);
    }

    public static <T> Specification<T> toSpecification(final String rsqlQuery, final List<RSQLCustomPredicate<?>> customPredicates) {
        return toSpecification(rsqlQuery, false, null, customPredicates);
    }

    public static <T> Specification<T> toSpecification(final String rsqlQuery, final boolean distinct, final Map<String, String> propertyPathMapper, final List<RSQLCustomPredicate<?>> customPredicates) {
        log.debug("toSpecification({},distinct:{},propertyPathMapper:{})", rsqlQuery, distinct, propertyPathMapper);
        return (root, query, cb) -> {
            query.distinct(distinct);
            if (StringUtils.hasText(rsqlQuery)) {
                Set<ComparisonOperator> supportedOperators = RSQLOperators.supportedOperators();
                if (customPredicates != null) {
                    supportedOperators.addAll(customPredicates.stream().map(RSQLCustomPredicate::getOperator).filter(Objects::nonNull).collect(Collectors.toSet()));
                }
                Node rsql = new RSQLParser(supportedOperators).parse(rsqlQuery);
                return rsql.accept(new ArchbaseRSQLJPAPredicateConverter(cb, propertyPathMapper, customPredicates), root);
            } else
                return null;
        };
    }

    public static <T> Specification<T> toSort(@Nullable final String sortQuery) {
        return toSort(sortQuery, new HashMap<>());
    }

    /**
     * Adicione orderBy (s) a {@code CriteriaQuery}.
     * Examplo: {@code "field1,asc;field2,desc;field3.subfield1,asc"}
     *
     * @param sortQuery          - consulta de classificação
     * @param propertyPathMapper - remapeamento de propriedade
     * @param <T>
     * @return {@code Specification} com pedido especificado por
     * @author edsonmartins
     */
    public static <T> Specification<T> toSort(@Nullable final String sortQuery, final Map<String, String> propertyPathMapper) {
        log.debug("toSort({},propertyPathMapper:{})", sortQuery, propertyPathMapper);
        return (root, query, cb) -> {
            if (StringUtils.hasText(sortQuery)) {
                final List<Order> orders = SortUtils.parseSort(sortQuery, propertyPathMapper, root, cb);
                query.orderBy(orders);

                return cb.conjunction();
            } else
                return null;
        };
    }

    /**
     * Retorna uma única entidade que corresponde à {@link Specification} fornecida ou {@link Optional#empty()}
     * se nenhuma for encontrada.
     *
     * @param jpaSpecificationExecutor repositório JPA
     * @param rsqlQuery                pode ser {@literal null}.
     * @return never {@literal null}.
     * @throws org.springframework.dao.IncorrectResultSizeDataAccessException se mais de uma entidade encontrada.
     */
    public static Optional findOne(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
        return jpaSpecificationExecutor.findOne(toSpecification(rsqlQuery));
    }

    /**
     * Retorna todas as entidades que correspondem à {@link Specification} fornecida.
     *
     * @param jpaSpecificationExecutor repositório JPA
     * @param rsqlQuery                pode ser {@literal null}.
     * @return never {@literal null}.
     */
    public static List findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
        return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery));
    }

    /**
     * Retorna uma {@link Page} de entidades que correspondem à {@link Specification} fornecida.
     *
     * @param jpaSpecificationExecutor repositório JPA
     * @param rsqlQuery                pode ser {@literal null}.
     * @param pageable                 não pode ser {@literal null}.
     * @return never {@literal null}.
     */
    public static Page findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, Pageable pageable) {
        return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), pageable);
    }

    /**
     * Retorna todas as entidades que correspondem à {@link Specification} e {@link Sort} fornecidas.
     *
     * @param jpaSpecificationExecutor repositório JPA
     * @param rsqlQuery                pode ser {@literal null}.
     * @param sort                     não pode ser {@literal null}.
     * @return never {@literal null}.
     */
    public static List findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, Sort sort) {
        return jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), sort);
    }

    /**
     * Retorna todas as entidades que correspondem à {@link Specification} e {@link Sort} fornecidas.
     *
     * @param jpaSpecificationExecutor repositório JPA
     * @param rsqlQuery                pode ser {@literal null}.
     * @param sort                     pode ser {@literal null}, delimitado por vírgulas.
     * @return never {@literal null}.
     */
    public static List findAll(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery, @Nullable String sort) {
        return StringUtils.hasText(sort)
                ? jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery), Sort.by(Direction.ASC, StringUtils.commaDelimitedListToStringArray(sort)))
                : jpaSpecificationExecutor.findAll(toSpecification(rsqlQuery));
    }

    /**
     * Retorna o número de instâncias que a {@link Specification} fornecida retornará.
     *
     * @param jpaSpecificationExecutor repositório JPA
     * @param rsqlQuery                A {@link Specification} para contar instâncias. Pode ser {@literal null}.
     * @return o número de instâncias.
     */
    public static long count(JpaSpecificationExecutor<?> jpaSpecificationExecutor, @Nullable String rsqlQuery) {
        return jpaSpecificationExecutor.count(toSpecification(rsqlQuery));
    }

}
