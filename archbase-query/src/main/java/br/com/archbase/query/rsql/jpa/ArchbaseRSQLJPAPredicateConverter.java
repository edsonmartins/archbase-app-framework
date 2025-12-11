package br.com.archbase.query.rsql.jpa;

import br.com.archbase.query.rsql.common.RSQLCustomPredicate;
import br.com.archbase.query.rsql.common.RSQLCustomPredicateInput;
import br.com.archbase.query.rsql.common.RSQLVisitorBase;
import br.com.archbase.query.rsql.parser.ast.AndNode;
import br.com.archbase.query.rsql.parser.ast.ComparisonNode;
import br.com.archbase.query.rsql.parser.ast.ComparisonOperator;
import br.com.archbase.query.rsql.parser.ast.OrNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.ManagedType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static br.com.archbase.query.rsql.common.RSQLOperators.*;

@Slf4j
@SuppressWarnings("all")
public class ArchbaseRSQLJPAPredicateConverter extends RSQLVisitorBase<Predicate, Root> {

    private final CriteriaBuilder builder;
    private final Map<String, Path> cachedJoins = new HashMap<>();
    private final @Getter
    Map<String, String> propertyPathMapper;
    private final @Getter
    Map<ComparisonOperator, RSQLCustomPredicate<?>> customPredicates;

    public ArchbaseRSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper) {
        this(builder, propertyPathMapper, null);
    }

    public ArchbaseRSQLJPAPredicateConverter(CriteriaBuilder builder, Map<String, String> propertyPathMapper, List<RSQLCustomPredicate<?>> customPredicates) {
        super();
        this.builder = builder;
        this.propertyPathMapper = propertyPathMapper != null ? propertyPathMapper : Collections.emptyMap();
        this.customPredicates = customPredicates != null ? customPredicates.stream().collect(Collectors.toMap(RSQLCustomPredicate::getOperator, Function.identity(), (a, b) -> a)) : Collections.emptyMap();
    }

    <T> ArchbaseRSQLJPAContext findPropertyPath(String propertyPath, Path startRoot) {
        ManagedType<?> classMetadata = getManagedType(startRoot.getJavaType());
        Path<?> root = startRoot;
        Class type = startRoot.getJavaType();
        Attribute<?, ?> attribute = null;

        String[] properties = propertyPath.split("\\.");

        for (String property : properties) {
            String mappedProperty = mapProperty(property, classMetadata.getJavaType());
            if (!mappedProperty.equals(property)) {
                ArchbaseRSQLJPAContext context = findPropertyPath(mappedProperty, root);
                root = context.getPath();
                attribute = context.getAttribute();
            } else {
                if (!hasPropertyName(mappedProperty, classMetadata)) {
                    throw new IllegalArgumentException("Propriedade desconhecida: " + mappedProperty + " da entidade " + classMetadata.getJavaType().getName());
                }

                if (isAssociationType(mappedProperty, classMetadata)) {
                    boolean isOneToAssociationType = isOneToOneAssociationType(mappedProperty, classMetadata) || isOneToManyAssociationType(mappedProperty, classMetadata);
                    Class<?> associationType = findPropertyType(mappedProperty, classMetadata);
                    type = associationType;
                    String previousClass = classMetadata.getJavaType().getName();
                    classMetadata = getManagedType(associationType);

                    String keyJoin = root.getJavaType().getSimpleName().concat(".").concat(mappedProperty);
                    log.debug("Crie uma junção entre [{}] e [{}] usando a chave [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
                    root = isOneToAssociationType ? joinLeft(keyJoin, root, mappedProperty) : join(keyJoin, root, mappedProperty);
                } else if (isElementCollectionType(mappedProperty, classMetadata)) {
                    String previousClass = classMetadata.getJavaType().getName();
                    attribute = classMetadata.getAttribute(property);
                    classMetadata = getManagedElementCollectionType(mappedProperty, classMetadata);

                    String keyJoin = root.getJavaType().getSimpleName().concat(".").concat(mappedProperty);
                    log.debug("Crie uma junção de coleção de elementos entre [{}] e [{}] usando a chave [{}]", previousClass, classMetadata.getJavaType().getName(), keyJoin);
                    root = join(keyJoin, root, mappedProperty);
                } else {
                    log.debug("Crie o caminho da propriedade para o tipo [{}] propriedade [{}]", classMetadata.getJavaType().getName(), mappedProperty);
                    root = root.get(mappedProperty);

                    if (isEmbeddedType(mappedProperty, classMetadata)) {
                        Class<?> embeddedType = findPropertyType(mappedProperty, classMetadata);
                        type = embeddedType;
                        classMetadata = getManagedType(embeddedType);
                    } else {
                        attribute = classMetadata.getAttribute(property);
                    }
                }
            }
        }
        accessControl(type, attribute.getName());
        return ArchbaseRSQLJPAContext.of(root, attribute);
    }

    protected Path<?> join(String keyJoin, Path<?> root, String mappedProperty) {
        log.info("join(keyJoin:{},root:{},mappedProperty:{})", keyJoin, root, mappedProperty);

        if (cachedJoins.containsKey(keyJoin)) {
            root = cachedJoins.get(keyJoin);
        } else {
            root = ((From) root).join(mappedProperty, JoinType.LEFT);
            cachedJoins.put(keyJoin, root);
        }
        return root;
    }

    protected Path<?> joinLeft(String keyJoin, Path<?> root, String mappedProperty) {
        log.info("joinLeft(keyJoin:{},root:{},mappedProperty:{})", keyJoin, root, mappedProperty);

        if (cachedJoins.containsKey(keyJoin)) {
            root = cachedJoins.get(keyJoin);
        } else {
            root = ((From) root).join(mappedProperty, JoinType.LEFT);
            cachedJoins.put(keyJoin, root);
        }
        return root;
    }

    @Override
    public Predicate visit(ComparisonNode node, Root root) {
        log.debug("visit(node:{},root:{})", node, root);

        ComparisonOperator op = node.getOperator();
        ArchbaseRSQLJPAContext holder = findPropertyPath(mapPropertyPath(node.getSelector()), root);
        Path attrPath = holder.getPath();
        Attribute attribute = holder.getAttribute();
        Class type = attribute.getJavaType();
        if (attribute.getPersistentAttributeType() == PersistentAttributeType.ELEMENT_COLLECTION) {
            type = getElementCollectionGenericType(type, attribute);
        }
        if (type.isPrimitive()) {
            type = primitiveToWrapper.get(type);
        } else if (ArchbaseRSQLJPASupport.getValueTypeMap().containsKey(type)) {
            type = ArchbaseRSQLJPASupport.getValueTypeMap().get(type); // se você quiser tratar Enum como String e aplicar como pesquisa, etc.
        }

        if (customPredicates.containsKey(op)) {
            RSQLCustomPredicate<?> customPredicate = customPredicates.get(op);
            List<Object> arguments = new ArrayList<>();
            for (String argument : node.getArguments()) {
                arguments.add(convert(argument, customPredicate.getType()));
            }
            return customPredicate.getConverter().apply(RSQLCustomPredicateInput.of(builder, attrPath, arguments));
        }

        if (node.getArguments().size() > 1) {
            List<Object> listObject = new ArrayList<>();
            for (String argument : node.getArguments()) {
                listObject.add(convert(argument, type));
            }
            if (op.equals(IN)) {
                return attrPath.in(listObject);
            }
            if (op.equals(NOT_IN)) {
                return attrPath.in(listObject).not();
            }
            if (op.equals(BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
                return builder.between(attrPath, (Comparable) listObject.get(0), (Comparable) listObject.get(1));
            }
            if (op.equals(NOT_BETWEEN) && listObject.size() == 2 && listObject.get(0) instanceof Comparable && listObject.get(1) instanceof Comparable) {
                return builder.between(attrPath, (Comparable) listObject.get(0), (Comparable) listObject.get(1)).not();
            }
        } else {
            if (op.equals(IS_NULL)) {
                return builder.isNull(attrPath);
            }
            if (op.equals(NOT_NULL)) {
                return builder.isNotNull(attrPath);
            }
            Object argument = convert(node.getArguments().get(0), type);
            if (op.equals(IN)) {
                return builder.equal(attrPath, argument);
            }
            if (op.equals(NOT_IN)) {
                return builder.notEqual(attrPath, argument);
            }
            if (op.equals(LIKE)) {
                return builder.like(attrPath, "%" + argument.toString() + "%");
            }
            if (op.equals(NOT_LIKE)) {
                return builder.like(attrPath, "%" + argument.toString() + "%").not();
            }
            if (op.equals(IGNORE_CASE)) {
                return builder.equal(builder.upper(attrPath), argument.toString().toUpperCase());
            }
            if (op.equals(IGNORE_CASE_LIKE)) {
                return builder.like(builder.upper(attrPath), "%" + argument.toString().toUpperCase() + "%");
            }
            if (op.equals(IGNORE_CASE_NOT_LIKE)) {
                return builder.like(builder.upper(attrPath), "%" + argument.toString().toUpperCase() + "%").not();
            }
            if (op.equals(EQUAL)) {
                if (type.equals(String.class)) {
                    if (argument.toString().contains("*") && argument.toString().contains("^")) {
                        return builder.like(builder.upper(attrPath), argument.toString().replace("*", "%").replace("^", "").toUpperCase());
                    } else if (argument.toString().contains("*")) {
                        return builder.like(attrPath, argument.toString().replace('*', '%'));
                    } else if (argument.toString().contains("^")) {
                        return builder.equal(builder.upper(attrPath), argument.toString().replace("^", "").toUpperCase());
                    } else {
                        return builder.equal(attrPath, argument);
                    }
                } else if (argument == null) {
                    log.warn("Filtro EQUAL ignorado: valor incompatível com o tipo do campo");
                    return builder.disjunction();  // Retorna FALSE, ignorando este filtro em OR
                } else {
                    return builder.equal(attrPath, argument);
                }
            }
            if (op.equals(NOT_EQUAL)) {
                if (type.equals(String.class)) {
                    if (argument.toString().contains("*") && argument.toString().contains("^")) {
                        return builder.notLike(builder.upper(attrPath), argument.toString().replace("*", "%").replace("^", "").toUpperCase());
                    } else if (argument.toString().contains("*")) {
                        return builder.notLike(attrPath, argument.toString().replace('*', '%'));
                    } else if (argument.toString().contains("^")) {
                        return builder.notEqual(builder.upper(attrPath), argument.toString().replace("^", "").toUpperCase());
                    } else {
                        return builder.notEqual(attrPath, argument);
                    }
                } else if (argument == null) {
                    log.warn("Filtro NOT_EQUAL ignorado: valor incompatível com o tipo do campo");
                    return builder.disjunction();  // Retorna FALSE, ignorando este filtro em OR
                } else {
                    return builder.notEqual(attrPath, argument);
                }
            }
            if (!Comparable.class.isAssignableFrom(type)) {
                log.error("Operator {} pode ser usado apenas para comparáveis", op);
                throw new IllegalArgumentException(String.format("O operador %s pode ser usado apenas para comparáveis", op));
            }
            Comparable comparable = (Comparable) argument;

            if (op.equals(GREATER_THAN)) {
                return builder.greaterThan(attrPath, comparable);
            }
            if (op.equals(GREATER_THAN_OR_EQUAL)) {
                return builder.greaterThanOrEqualTo(attrPath, comparable);
            }
            if (op.equals(LESS_THAN)) {
                return builder.lessThan(attrPath, comparable);
            }
            if (op.equals(LESS_THAN_OR_EQUAL)) {
                return builder.lessThanOrEqualTo(attrPath, comparable);
            }
        }
        log.error("Operador desconhecido: {}", op);
        throw new IllegalArgumentException("Operador desconhecido: " + op);
    }

    @Override
    public Predicate visit(AndNode node, Root root) {
        log.debug("visit(node:{},root:{})", node, root);

        return node.getChildren().stream().map(n -> n.accept(this, root)).collect(Collectors.reducing(builder::and)).get();
    }

    @Override
    public Predicate visit(OrNode node, Root root) {
        log.debug("visit(node:{},root:{})", node, root);

        return node.getChildren().stream().map(n -> n.accept(this, root)).collect(Collectors.reducing(builder::or)).get();
    }

}
