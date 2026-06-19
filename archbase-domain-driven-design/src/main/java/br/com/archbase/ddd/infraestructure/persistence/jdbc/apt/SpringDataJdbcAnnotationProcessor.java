package br.com.archbase.ddd.infraestructure.persistence.jdbc.apt;

import com.querydsl.apt.AbstractQuerydslProcessor;
import com.querydsl.apt.Configuration;
import com.querydsl.apt.TypeElementHandler;
import com.querydsl.codegen.*;
import org.springframework.data.annotation.Id;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("org.springframework.data.annotation.Id")
public class SpringDataJdbcAnnotationProcessor extends AbstractQuerydslProcessor {

    private RoundEnvironment roundEnv;
    private CustomExtendedTypeFactory typeFactory;
    private Configuration conf;

    @Override
    protected Configuration createConfiguration(RoundEnvironment roundEnv) {
        Class<? extends Annotation> entity = Id.class;
        this.roundEnv = roundEnv;
        CodegenModule codegenModule = new CodegenModule();
        JavaTypeMappings typeMappings = new JavaTypeMappings();
        codegenModule.bind(TypeMappings.class, typeMappings);
        codegenModule.bind(QueryTypeFactory.class, new QueryTypeFactoryImpl("", "", ""));
        SpringDataJdbcConfiguration springDataJdbcConfiguration = new SpringDataJdbcConfiguration(roundEnv,
                processingEnv,
                entity, null, null,
                null, Ignored.class,
                typeMappings,
                codegenModule);
        this.conf = springDataJdbcConfiguration;
        return springDataJdbcConfiguration;
    }

    @Override
    protected TypeElementHandler createElementHandler(TypeMappings typeMappings, QueryTypeFactory queryTypeFactory) {
        return new CustomElementHandler(conf, typeFactory, typeMappings, queryTypeFactory, processingEnv.getElementUtils(), roundEnv, processingEnv.getMessager());
    }

    @Override
    protected CustomExtendedTypeFactory createTypeFactory(Set<Class<? extends Annotation>> entityAnnotations,
                                                          TypeMappings typeMappings,
                                                          QueryTypeFactory queryTypeFactory) {
        CustomExtendedTypeFactory customExtendedTypeFactory = new CustomExtendedTypeFactory(processingEnv,
                entityAnnotations,
                typeMappings,
                queryTypeFactory,
                conf.getVariableNameFunction());
        this.typeFactory = customExtendedTypeFactory;
        return customExtendedTypeFactory;
    }

    @Override
    protected Set<TypeElement> collectElements() {
        return roundEnv.getElementsAnnotatedWith(conf.getEntityAnnotation())
                .stream()
                .map(Element::getEnclosingElement)
                .filter(element -> element instanceof TypeElement)
                .map(element -> (TypeElement) element)
                .collect(Collectors.toSet());
    }

    @Override
    protected String getClassName(EntityType model) {
        return model.getFullName();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Ignored {
    }
}