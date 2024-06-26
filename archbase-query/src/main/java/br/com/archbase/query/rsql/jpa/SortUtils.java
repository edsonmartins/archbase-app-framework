package br.com.archbase.query.rsql.jpa;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author edsonmartins
 */
public class SortUtils {

    private static final String MULTIPLE_SORT_SEPARATOR = ";";
    private static final String SORT_SEPARATOR = ":";
    private static final String PROPERTY_PATH_SEPARATOR = "\\.";
    private SortUtils() {
    }

    public static List<Order> parseSort(@Nullable final String sort, final Map<String, String> propertyMapper, final Root<?> root, final CriteriaBuilder cb) {
        if (sort == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(sort.split(MULTIPLE_SORT_SEPARATOR))
                .map(item -> item.split(SORT_SEPARATOR))
                .map(parts -> sortToJpaOrder(parts, propertyMapper, root, cb))
                .collect(Collectors.toList());
    }

    public static List<org.springframework.data.domain.Sort.Order> convertSortToJpa(String[] sort) {
        List<org.springframework.data.domain.Sort.Order> orders = new ArrayList<>();
        for (String sortOrder : sort) {
            String[] sortTmp = sortOrder.split(SORT_SEPARATOR);
            orders.add(new org.springframework.data.domain.Sort.Order(getSortDirection(sortTmp.length > 1 ? sortTmp[1] : null), sortTmp[0]));
        }
        return orders;
    }

    public static Sort.Direction getSortDirection(String direction) {
        if ("asc".equals(direction)) {
            return Sort.Direction.ASC;
        } else if ("desc".equals(direction)) {
            return Sort.Direction.DESC;
        }

        return Sort.Direction.ASC;
    }

    private static Order sortToJpaOrder(final String[] parts, final Map<String, String> propertyMapper, final Root<?> root, final CriteriaBuilder cb) {
        final String property = parts[0];
        final String direction = parts[1];

        final String propertyPath = propertyMapper.getOrDefault(property, property);
        final Expression<?> propertyExpression = pathToExpression(root, propertyPath);
        return direction.equalsIgnoreCase("asc") ? cb.asc(propertyExpression) : cb.desc(propertyExpression);
    }

    private static Expression<?> pathToExpression(final Root<?> root, final String path) {
        final String[] properties = path.split(PROPERTY_PATH_SEPARATOR);

        Path<?> expression = root.get(properties[0]);
        for (int i = 1; i < properties.length; ++i) {
            expression = expression.get(properties[i]);
        }
        return expression;
    }

}

