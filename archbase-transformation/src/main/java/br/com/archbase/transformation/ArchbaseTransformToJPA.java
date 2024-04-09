package br.com.archbase.transformation;

import br.com.archbase.ddd.domain.contracts.Identifiable;
import br.com.archbase.ddd.persistence.annotations.PersistenceDomainEntity;
import br.com.archbase.ddd.persistence.annotations.PersistenceDomainIdentifier;
import br.com.archbase.ddd.persistence.annotations.PersistenceDomainValueObject;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.MemberAttributeExtension;
import net.bytebuddy.build.Plugin.NoOp;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.matcher.ElementMatchers;

import jakarta.persistence.Embeddable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Plugin para aplicar as anotações de persistência do metamodel nas entidades e agregados.
 * Isso foi uma técnica usada para não poluir as classes do dominio com anotações de persistência e
 * também desacoplá-las da persistência. Para trocar a forma de persistência basta apenas criar um novo
 * metamodel para o formato desejado e escrever um novo plugin.
 * <p>
 * Desta formas as entidades do domínio podem ser transformadas para persistir o formato que desejarmos.
 *
 * @author edsonmartins
 */

public class ArchbaseTransformToJPA extends NoOp {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArchbaseTransformToJPA.class);

    private static Set<Class<?>> jpaModelAnnotations = new HashSet<>(
            Arrays.asList(PersistenceDomainEntity.class, PersistenceDomainIdentifier.class, PersistenceDomainValueObject.class));

    private static Builder<?> handleAggregateRoot(Builder<?> builder, TypeDescription type) {

        if (!type.isAssignableTo(Serializable.class)) {

            log.info("Archbase JPA Transformation Plugin - Adicionando Serializable para {}.", type.getName());

            builder = builder.implement(Serializable.class);
        }

        return addAnnotationIfMissing(Embeddable.class, builder, type);
    }

    private static Builder<?> handleValueObject(Builder<?> builder, TypeDescription type) {
        return addAnnotationIfMissing(Embeddable.class, builder, type);
    }

    private static Builder<?> handleEntity(Builder<?> builder, TypeDescription type) {
        AnnotationDescription.Loadable<PersistenceDomainEntity> annotationDescription = type.getDeclaredAnnotations().ofType(PersistenceDomainEntity.class);
        Class<? extends Identifiable> aClass = annotationDescription.load().value();
        Builder<? extends Identifiable> rebase = new ByteBuddy()
                .rebase(aClass);
        TypeDescription typeDescription = rebase.toTypeDescription();
        /**
         * Clone anotações da persistência do modelo de dados para a entidade de domínio
         */
        for (AnnotationDescription annd : type.getDeclaredAnnotations()) {
            if (!(jpaModelAnnotations.stream().anyMatch(it -> annd.getAnnotationType().isAssignableTo(it) &&
                    !typeDescription.getDeclaredAnnotations().isAnnotationPresent(annd.getAnnotationType())))) {
                rebase = rebase.annotateType(annd);
            }
        }

        /**
         * Clone anotações de campos de modelo de dados para objeto de valor de domínio na entidade de domínio
         */
        for (FieldDescription field : type.getDeclaredFields()) {
            for (AnnotationDescription annd : field.getDeclaredAnnotations()) {
                Optional<FieldDescription.InDefinedShape> targetField = typeDescription.getDeclaredFields().stream().parallel().filter(fld -> fld.getName().equals(field.getName())).findFirst();
                if (targetField.isPresent() && !targetField.get().getDeclaredAnnotations().isAnnotationPresent(annd.getAnnotationType())) {
                    rebase = rebase.visit(new MemberAttributeExtension.ForField().annotate(annd).on(ElementMatchers.named(field.getName())));
                }
            }
        }

        try {
            rebase.make().saveIn(new File(aClass.getProtectionDomain().getCodeSource().getLocation().getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder;
    }

    private static Builder<?> addAnnotationIfMissing(Class<? extends Annotation> annotation, Builder<?> builder,
                                                     TypeDescription type) {

        if (type.getDeclaredAnnotations().isAnnotationPresent(annotation)) {
            return builder;
        }

        log.info("Archbase JPA Plugin - Anotando {} com {}.", type.getName(), annotation.getName());

        return builder.annotateType(getAnnotation(annotation));
    }

    private static AnnotationDescription getAnnotation(Class<? extends Annotation> type) {
        return AnnotationDescription.Builder.ofType(type).build();
    }

    private static boolean isAnnotatedWith(TypeDescription type, Class<?> annotationType) {
        return type.getDeclaredAnnotations() //
                .asTypeList() //
                .stream() //
                .anyMatch(it -> it.isAssignableTo(annotationType)); //
    }

    @Override
    public boolean matches(TypeDescription target) {
        String message = target.toString();
        log.info(message);
        return (jpaModelAnnotations.stream().anyMatch(it -> isAnnotatedWith(target, it)));
    }

    /**
     * (non-Javadoc)
     *
     * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
     */
    @Override
    public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

        if (isAnnotatedWith(type, PersistenceDomainEntity.class)) {
            builder = handleEntity(builder, type);
        }

        if (type.isAssignableTo(PersistenceDomainValueObject.class)) {
            builder = handleAggregateRoot(builder, type);
        }

        if (type.isAssignableTo(PersistenceDomainIdentifier.class)) {
            builder = handleValueObject(builder, type);
        }

        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}