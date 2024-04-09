package br.com.archbase.transformation;

import br.com.archbase.ddd.domain.annotations.DomainFactory;
import br.com.archbase.ddd.domain.annotations.DomainRepository;
import br.com.archbase.ddd.domain.annotations.DomainService;
import net.bytebuddy.build.Plugin.NoOp;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class ArchbaseTransformToSpring extends NoOp {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArchbaseTransformToSpring.class);

    private static Set<Class<?>> annotations = new HashSet<>(
            Arrays.asList(DomainService.class, DomainRepository.class, DomainFactory.class));

    private static Builder<?> addAnnotationIfMissing(Class<? extends Annotation> annotation, Builder<?> builder,
                                                     TypeDescription type) {

        if (isAnnotatedWith(type, annotation)) {
            return builder;
        }

        log.info("Archbase Spring Transformation Plugin - Anotando {} com {}.", type.getName(), annotation.getName());

        return builder.annotateType(getAnnotation(annotation));
    }

    private static AnnotationDescription getAnnotation(Class<? extends Annotation> type) {
        return AnnotationDescription.Builder.ofType(type).build();
    }

    private static boolean isAnnotatedWith(TypeDescription type, Class<?> annotationType) {

        return type.getDeclaredAnnotations() //
                .asTypeList() //
                .stream() //
                .anyMatch(it -> it.isAssignableTo(annotationType));
    }

    @Override
    public boolean matches(TypeDescription target) {
        return annotations.stream().anyMatch(it -> isAnnotatedWith(target, it));
    }

    /*
     * (non-Javadoc)
     * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
     */
    @Override
    public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

        if (isAnnotatedWith(type, DomainService.class)) {
            builder = addAnnotationIfMissing(org.springframework.stereotype.Service.class, builder, type);
        }

        if (isAnnotatedWith(type, DomainRepository.class)) {
            builder = addAnnotationIfMissing(org.springframework.stereotype.Repository.class, builder, type);
        }

        if (isAnnotatedWith(type, DomainFactory.class)) {
            builder = addAnnotationIfMissing(org.springframework.stereotype.Component.class, builder, type);
        }

        return builder;
    }
}