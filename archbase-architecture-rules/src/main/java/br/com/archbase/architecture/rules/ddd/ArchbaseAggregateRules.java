package br.com.archbase.architecture.rules.ddd;

import br.com.archbase.ddd.domain.annotations.DomainAggregateRoot;
import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Entity;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaGenericArrayType;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Regras de modelagem de agregados, verificadas pelo <b>tipo</b> e pelas <b>anotações</b> de DDD — e não pelo nome da
 * classe, como faz {@link ArchbaseDddRules#aggregatesShouldNotReferenceOtherAggregatesDirectly()}.
 * <p>
 * Equivalem às regras de {@code br.com.archbase.validation.ddd.model.ArchbaseRules} (módulo
 * {@code archbase-validation-ddd-model}), que vieram do jMolecules mas ficaram inertes na migração para o ArchUnit 1.x:
 * os predicados daquela classe sempre devolvem {@code false}, então nenhum campo é examinado. Aquela classe foi mantida
 * intacta para não mudar o resultado de quem já a usa; estas são a versão que funciona.
 * <p>
 * Só são examinados campos declarados em <em>tipos de domínio</em> — classes que implementam {@link Entity} /
 * {@link AggregateRoot} ou anotadas com {@link DomainEntity} / {@link DomainAggregateRoot}. Campos de DTOs, serviços e
 * controllers ficam de fora. Coleções, {@code Optional}, valores de {@code Map} e arrays são desembrulhados.
 * <p>
 * Nenhuma regra é ligada por padrão: use
 * {@link br.com.archbase.architecture.rules.core.ArchbaseArchitectureRules#withAggregateRules()}.
 *
 * @since 3.4.5
 */
public final class ArchbaseAggregateRules {

    private ArchbaseAggregateRules() {
    }

    /**
     * Todas as regras desta classe.
     */
    public static ArchRule all() {
        return CompositeArchRule
                .of(entitiesShouldBeDeclaredForUseInSameAggregate())
                .and(aggregateReferencesShouldBeViaIdOrAssociation());
    }

    /**
     * Um campo cujo tipo é uma {@link Entity} (que não seja raiz) só pode aparecer no agregado que essa entidade
     * declara como dono.
     * <pre>{@code
     * class LineItem implements Entity<Order, LineItemId> { … }
     * class Address  implements Entity<Customer, AddressId> { … }
     *
     * class Order implements AggregateRoot<Order, OrderId> {
     *     List<LineItem> lineItems;  // válido
     *     Address shippingAddress;   // inválido: Address pertence a Customer
     * }
     * }</pre>
     * Também vale para campos declarados em entidades internas: o dono considerado é o agregado da entidade.
     * Tipos marcados só por anotação não declaram o agregado dono e por isso não são verificados aqui.
     */
    public static ArchRule entitiesShouldBeDeclaredForUseInSameAggregate() {
        return ArchRuleDefinition.fields()
                .that(areDeclaredInDomainType())
                .should(new BelongToDeclaringAggregate())
                .allowEmptyShould(true)
                .as("Entidades só podem ser referenciadas dentro do agregado a que pertencem");
    }

    /**
     * Um tipo de domínio não guarda referência direta a uma raiz de agregado ({@link AggregateRoot} ou
     * {@link DomainAggregateRoot}). Deve guardar o identificador ou uma associação.
     * <pre>{@code
     * class Order implements AggregateRoot<Order, OrderId> {
     *     Customer customer;      // inválido
     *     CustomerId customerId;  // válido
     * }
     * }</pre>
     * Atenção: em modelos que usam a mesma classe como entidade JPA e agregado, cada {@code @ManyToOne} para outro
     * agregado é uma violação. É a regra de DDD — por isso ela é opcional.
     */
    public static ArchRule aggregateReferencesShouldBeViaIdOrAssociation() {
        return ArchRuleDefinition.fields()
                .that(areDeclaredInDomainType())
                .should(new NotReferenceAggregateRoot())
                .allowEmptyShould(true)
                .as("Agregados devem ser referenciados por identificador ou associação, não diretamente");
    }

    // ---------------------------------------------------------------- classificação

    static boolean isAggregateRoot(JavaClass type) {
        return type.isAssignableTo(AggregateRoot.class)
                || type.isAnnotatedWith(DomainAggregateRoot.class)
                || type.isMetaAnnotatedWith(DomainAggregateRoot.class);
    }

    static boolean isTypedEntity(JavaClass type) {
        return type.isAssignableTo(Entity.class) && !isAggregateRoot(type);
    }

    static boolean isDomainType(JavaClass type) {
        return isAggregateRoot(type)
                || type.isAssignableTo(Entity.class)
                || type.isAnnotatedWith(DomainEntity.class)
                || type.isMetaAnnotatedWith(DomainEntity.class);
    }

    private static DescribedPredicate<JavaField> areDeclaredInDomainType() {
        return DescribedPredicate.describe("são declarados em tipos de domínio",
                field -> !field.getModifiers().contains(JavaModifier.STATIC)
                        && !field.getModifiers().contains(JavaModifier.SYNTHETIC)
                        && isDomainType(field.getOwner()));
    }

    /**
     * O agregado dono de um tipo de domínio: ele mesmo, se for raiz; o primeiro argumento de {@code Entity<T, ID>},
     * se for entidade tipada; vazio se não for possível determinar.
     */
    static Optional<JavaClass> owningAggregate(JavaClass type) {
        if (isAggregateRoot(type)) {
            return Optional.of(type);
        }
        return findTypeArgument(type, Entity.class.getName(), 0, Map.of()).flatMap(ArchbaseAggregateRules::erasure);
    }

    // ---------------------------------------------------------------- genéricos

    /**
     * Procura, na hierarquia de {@code type}, o argumento {@code index} do supertipo {@code targetName}, resolvendo
     * variáveis de tipo pelo caminho (ex.: {@code Item extends DomainEntityBase<Pedido>} e
     * {@code DomainEntityBase<T> implements Entity<T, Identifier>} resolvem para {@code Pedido}).
     */
    private static Optional<JavaType> findTypeArgument(JavaClass type, String targetName, int index,
                                                       Map<String, JavaType> bindings) {
        List<JavaType> supertypes = new ArrayList<>();
        type.getSuperclass().ifPresent(supertypes::add);
        supertypes.addAll(type.getInterfaces());

        for (JavaType supertype : supertypes) {
            JavaClass raw = supertype.toErasure();
            Map<String, JavaType> next = new HashMap<>();
            List<JavaType> args = supertype instanceof JavaParameterizedType parameterized
                    ? parameterized.getActualTypeArguments() : List.of();
            List<? extends JavaTypeVariable<?>> params = raw.getTypeParameters();
            for (int i = 0; i < Math.min(args.size(), params.size()); i++) {
                next.put(params.get(i).getName(), bind(args.get(i), bindings));
            }
            if (raw.getName().equals(targetName)) {
                return index < args.size() ? Optional.of(bind(args.get(index), bindings)) : Optional.empty();
            }
            Optional<JavaType> found = findTypeArgument(raw, targetName, index, next);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static JavaType bind(JavaType type, Map<String, JavaType> bindings) {
        if (type instanceof JavaTypeVariable<?> variable && bindings.containsKey(variable.getName())) {
            return bindings.get(variable.getName());
        }
        return type;
    }

    /** Tipo concreto de um {@link JavaType}; vazio para variável de tipo não resolvida. */
    private static Optional<JavaClass> erasure(JavaType type) {
        if (type instanceof JavaTypeVariable<?>) {
            return Optional.empty();
        }
        return Optional.of(type.toErasure());
    }

    /** O tipo "de domínio" de um campo: elemento de coleção/Optional/array ou valor de Map; senão o próprio tipo. */
    static Optional<JavaClass> domainTypeOf(JavaField field) {
        JavaType type = field.getType();
        if (type instanceof JavaGenericArrayType array) {
            return erasure(array.getComponentType());
        }
        JavaClass raw = type.toErasure();
        if (raw.isArray()) {
            return Optional.of(raw.getBaseComponentType());
        }
        if (type instanceof JavaParameterizedType parameterized) {
            List<JavaType> args = parameterized.getActualTypeArguments();
            if (raw.isAssignableTo(Map.class) && args.size() == 2) {
                return erasure(args.get(1));
            }
            if ((raw.isAssignableTo(Iterable.class) || raw.isAssignableTo(Optional.class)) && args.size() == 1) {
                return erasure(args.get(0));
            }
        }
        return Optional.of(raw);
    }

    // ---------------------------------------------------------------- condições

    private static final class BelongToDeclaringAggregate extends ArchCondition<JavaField> {

        BelongToDeclaringAggregate() {
            super("pertencer ao agregado em que o campo é declarado");
        }

        @Override
        public void check(JavaField field, ConditionEvents events) {
            Optional<JavaClass> fieldType = domainTypeOf(field);
            if (fieldType.isEmpty() || !isTypedEntity(fieldType.get())) {
                return;
            }
            Optional<JavaClass> expected = owningAggregate(fieldType.get());
            Optional<JavaClass> owner = owningAggregate(field.getOwner());
            if (expected.isEmpty() || owner.isEmpty()) {
                return;
            }
            if (!expected.get().isAssignableTo(owner.get().getName())) {
                events.add(SimpleConditionEvent.violated(field, String.format(
                        "O campo %s.%s é do tipo %s, que pertence ao agregado %s, mas está no agregado %s",
                        field.getOwner().getName(), field.getName(), fieldType.get().getSimpleName(),
                        expected.get().getSimpleName(), owner.get().getSimpleName())));
            }
        }
    }

    private static final class NotReferenceAggregateRoot extends ArchCondition<JavaField> {

        NotReferenceAggregateRoot() {
            super("referenciar agregados por identificador ou associação");
        }

        @Override
        public void check(JavaField field, ConditionEvents events) {
            Optional<JavaClass> fieldType = domainTypeOf(field);
            if (fieldType.isPresent() && isAggregateRoot(fieldType.get())) {
                events.add(SimpleConditionEvent.violated(field, String.format(
                        "O campo %s.%s referencia diretamente a raiz de agregado %s. "
                                + "Use o identificador ou uma associação.",
                        field.getOwner().getName(), field.getName(), fieldType.get().getSimpleName())));
            }
        }
    }
}
