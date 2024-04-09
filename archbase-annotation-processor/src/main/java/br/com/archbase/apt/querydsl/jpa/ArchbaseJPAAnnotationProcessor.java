package br.com.archbase.apt.querydsl.jpa;

import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.annotations.DomainIdentifier;
import br.com.archbase.ddd.domain.annotations.DomainTransient;
import br.com.archbase.ddd.domain.annotations.DomainValueObject;
import com.querydsl.apt.AbstractQuerydslProcessor;
import com.querydsl.apt.Configuration;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import jakarta.persistence.MappedSuperclass;
import java.lang.annotation.Annotation;

/**
 * AnnotationProcessor para JPA que leva {@link DomainEntity}, {@link DomainValueObject}, {@link DomainIdentifier}
 * e {@link DomainTransient}  em conta
 *
 * @author edsonmartins
 */
@SupportedAnnotationTypes({"com.querydsl.core.annotations.*", "jakarta.persistence.*", "br.com.archbase.ddd.domain.annotations.*"})
public class ArchbaseJPAAnnotationProcessor extends AbstractQuerydslProcessor {

    @Override
    protected Configuration createConfiguration(RoundEnvironment roundEnv) {
        Class<? extends Annotation> entity = DomainEntity.class;
        Class<? extends Annotation> superType = MappedSuperclass.class;
        Class<? extends Annotation> embeddable = DomainValueObject.class;
        Class<? extends Annotation> embedded = DomainIdentifier.class;
        Class<? extends Annotation> skip = DomainTransient.class;
        return new ArchbaseJPAConfiguration(roundEnv, processingEnv,
                entity, superType, embeddable, embedded, skip);
    }

}