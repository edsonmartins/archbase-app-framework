package br.com.archbase.validation.ddd.model;

import br.com.archbase.ddd.domain.annotations.DomainAggregateRoot;
import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.*;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.springframework.core.ResolvableType;

import jakarta.persistence.Entity;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

import static com.tngtech.archunit.base.DescribedPredicate.not;


/**
 * Um conjunto de regras do ArchUnit que permite a verificação de modelos de domínio. Em suma, as regras aqui verificam:
 * <ul>
 * <li> Os agregados pessoais-se apenas a entidades que são declaradas como parte dele. </li>
 * <li> As referências a outros agregados são propriedade por meio de {@link Association} s ou referências de identificador. </li>
 * </ul>
 * Essas regras são principalmente pelo que foi apresentado por John Sullivan em sua postagem no blog
 * <a href="http://scabl.blogspot.com/2015/04/aeddd-9.html"> aqui </a>.
 *
 * @see http://scabl.blogspot.com/2015/04/aeddd-9.html
 */
public class ArchbaseRules {

    private ArchbaseRules() {
    }

    /**
     * Uma {@link ArchRule} regra composta por todas as outras regras declaradas nesta classe.
     *
     * @return
     * @see #entitiesShouldBeDeclaredForUseInSameAggregate()
     * @see #aggregateReferencesShouldBeViaIdOrAssociation()
     */
    public static ArchRule all() {

        return CompositeArchRule //
                .of(entitiesShouldBeDeclaredForUseInSameAggregate()) //
                .and(aggregateReferencesShouldBeViaIdOrAssociation());
    }

    /**
     * Um {@link ArchRule} que verifica se os campos que implementam {@link Entity} em um tipo de implementação
     * {@link AggregateRoot} declara o tipo de agregado como o agregado proprietário.
     * <p/>
     * <code>
     * class Customer implements AggregateRoot<Customer, CustomerId> { … }
     * class Address implements Entity<Customer, AddressId> { … }
     * <p>
     * class LineItem implements Entity<Order, LineItemId> { … }
     * class Order implements AggregateRoot<Order, OrderId> {
     * <p>
     * List<LineItem> lineItems; // válido
     * Address shippingAddress; // inválido porque o endereço foi declarado pertencer ao cliente
     * }
     * </code>
     *
     * @return nunca será {@literal null}.
     */
    public static ArchRule entitiesShouldBeDeclaredForUseInSameAggregate() {

        return ArchRuleDefinition.fields() //
                .that(areAssignableTo(Entity.class).and(not(areAssignableTo(AggregateRoot.class)))) //
                .should(beDeclaredToBeUsedWithDeclaringAggregate()); //
    }

    /**
     * Um {@link ArchRule} que garante que um {@link AggregateRoot} não faça referência a outro por meio do controle remoto
     * Tipo AggregateRoot, mas por meio de seu tipo de identificador ou um tipo {@link Association} explícito.
     * <p/>
     * <code>
     * class Customer implements AggregateRoot<Customer, CustomerId> { … }
     * <p>
     * class Order implements AggregateRoot<Order, OrderId> {
     * <p>
     * Customer customer; // inválido
     * CustomerId customerId; // válido
     * Association<Customer> customer; // válido
     * }
     * </code>
     *
     * @return nunca será {@literal null}.
     */
    public static ArchRule aggregateReferencesShouldBeViaIdOrAssociation() {

        return ArchRuleDefinition.fields() //
                .that(areAssignableTo(AggregateRoot.class))
                .or(hasFieldTypeAnnotatedWith(DomainAggregateRoot.class)) //
                .should(new ShouldUseIdReferenceOrAssociation());
    }

    private static IsDeclaredToUseTheSameAggregate beDeclaredToBeUsedWithDeclaringAggregate() {
        return new IsDeclaredToUseTheSameAggregate();
    }

    private static IsAssignableTypeField areAssignableTo(Class<?> type) {
        return new IsAssignableTypeField(type);
    }

    private static FieldTypeIsAnnotatedWith hasFieldTypeAnnotatedWith(Class<? extends Annotation> type) {
        return new FieldTypeIsAnnotatedWith(type);
    }

    private static class IsDeclaredToUseTheSameAggregate extends ArchCondition<JavaField> {

        private IsDeclaredToUseTheSameAggregate() {
            super("pertencem ao agregado em que o campo é declarado");
        }

        /*
         * (non-Javadoc)
         * @see com.tngtech.archunit.lang.ArchCondition#check(java.lang.Object, com.tngtech.archunit.lang.ConditionEvents)
         */
        @Override
        public void check(JavaField item, ConditionEvents events) {

            Field field = item.reflect();
            ResolvableType type = ResolvableType.forField(field);
            ResolvableType expectedAggregateType = type.as(Entity.class).getGeneric(0);
            ResolvableType owningType = ResolvableType.forClass(field.getDeclaringClass());

            String ownerName = FormatableJavaClass.of(item.getOwner()).getAbbreviatedFullName();

            events.add(owningType.isAssignableFrom(expectedAggregateType) ? SimpleConditionEvent.satisfied(field, "Matches")
                    : SimpleConditionEvent.violated(item,
                    String.format("O campo %s.%s é do tipo %s e declarado para ser usado do agregado %s!", ownerName,
                            item.getName(), item.getRawType().getSimpleName(),
                            expectedAggregateType.resolve(Object.class).getSimpleName())));
        }
    }

    private static class ShouldUseIdReferenceOrAssociation extends ArchCondition<JavaField> {

        public ShouldUseIdReferenceOrAssociation() {
            super("usar referência de id ou associação", new Object[0]);
        }

        /*
         * (non-Javadoc)
         * @see com.tngtech.archunit.lang.ArchCondition#check(java.lang.Object, com.tngtech.archunit.lang.ConditionEvents)
         */
        @Override
        public void check(JavaField field, ConditionEvents events) {

            events.add(SimpleConditionEvent.violated(field,
                    String.format(
                            "O campo %s.%s refere-se a uma raiz agregada (%s). Em vez disso, use uma referência de identificador ou associação!",
                            FormatableJavaClass.of(field.getOwner()).getAbbreviatedFullName(), field.getName(),
                            FormatableJavaClass.of(field.getRawType()).getAbbreviatedFullName())));
        }
    }

    private static class FieldTypeIsAnnotatedWith extends DescribedPredicate<JavaField> {

        private final DescribedPredicate<CanBeAnnotated> isAnnotatedWith;

        public FieldTypeIsAnnotatedWith(Class<? extends Annotation> type) {

            super("é do tipo anotado com %s", type.getSimpleName());

            this.isAnnotatedWith = CanBeAnnotated.Predicates.annotatedWith(type);
        }

//        /*
//         * (non-Javadoc)
//         * @see com.tngtech.archunit.base.Predicate#apply(java.lang.Object)
//         */
//        @Override
//        public boolean apply(JavaField input) {
//            return isAnnotatedWith.apply(input.getRawType());
//        }

        @Override
        public boolean test(JavaField javaField) {
            return false;
        }

        @Override
        public Predicate<JavaField> and(Predicate<? super JavaField> other) {
            return super.and(other);
        }

        @Override
        public Predicate<JavaField> negate() {
            return super.negate();
        }

        @Override
        public Predicate<JavaField> or(Predicate<? super JavaField> other) {
            return super.or(other);
        }
    }

    private static class IsAssignableTypeField extends DescribedPredicate<JavaField> {

        private static final ResolvableType COLLECTION_TYPE = ResolvableType.forClass(Collection.class);
        private static final ResolvableType MAP_TYPE = ResolvableType.forClass(Map.class);

        private final Class<?> type;

        private IsAssignableTypeField(Class<?> type) {
            super("são atribuíveis a %s", type.getName());
            this.type = type;
        }

        private static ResolvableType unwrapDomainType(ResolvableType fieldType) {

            if (COLLECTION_TYPE.isAssignableFrom(fieldType)) {
                return fieldType.as(Collection.class).getGeneric(0);
            }

            if (MAP_TYPE.isAssignableFrom(fieldType)) {
                return fieldType.as(Map.class).getGeneric(1);
            }

            return fieldType;
        }

        /*
         * (non-Javadoc)
         * @see com.tngtech.archunit.base.Predicate#apply(java.lang.Object)
         */
        public boolean apply(JavaField input) {

            ResolvableType fieldType = ResolvableType.forField(input.reflect());
            ResolvableType domainType = unwrapDomainType(fieldType);

            return ResolvableType.forClass(type).isAssignableFrom(domainType);
        }

        @Override
        public boolean test(JavaField javaField) {
            return false;
        }

        @Override
        public Predicate<JavaField> and(Predicate<? super JavaField> other) {
            return super.and(other);
        }

        @Override
        public Predicate<JavaField> negate() {
            return super.negate();
        }

        @Override
        public Predicate<JavaField> or(Predicate<? super JavaField> other) {
            return super.or(other);
        }
    }

}