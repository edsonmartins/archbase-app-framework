package br.com.archbase.ddd.infraestructure.persistence.jdbc.apt;

import com.querydsl.apt.DefaultConfiguration;
import com.querydsl.apt.VisitorConfig;
import com.querydsl.codegen.*;
import com.querydsl.core.annotations.QueryEntities;
import com.querydsl.sql.codegen.NamingStrategy;
import com.querydsl.sql.codegen.SQLCodegenModule;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.List;

import static com.querydsl.apt.VisitorConfig.FIELDS_ONLY;

@SuppressWarnings("all")
class SpringDataJdbcConfiguration extends DefaultConfiguration {

    private final SQLCodegenModule sqlCodegenModule;

    @SuppressWarnings("deprecation")
    public SpringDataJdbcConfiguration(RoundEnvironment roundEnv,
                                       ProcessingEnvironment processingEnv,
                                       Class<? extends Annotation> entityAnn,
                                       Class<? extends Annotation> superTypeAnn,
                                       Class<? extends Annotation> embeddableAnn,
                                       Class<? extends Annotation> embeddedAnn,
                                       Class<? extends Annotation> skipAnn,
                                       TypeMappings typeMappings,
                                       CodegenModule codegenModule) {
        super(processingEnv, roundEnv, Keywords.JPA, QueryEntities.class, entityAnn, superTypeAnn,
                embeddableAnn, embeddedAnn, skipAnn, codegenModule);
        setStrictMode(true);
        sqlCodegenModule = new SQLCodegenModule();
        sqlCodegenModule.bind(NamingStrategy.class, SpringDataJdbcQuerydslNamingStrategy.class);
        sqlCodegenModule.bind(TypeMappings.class, typeMappings);
    }

    @Override
    public VisitorConfig getConfig(TypeElement e, List<? extends Element> elements) {
        return FIELDS_ONLY;
    }

    @Override
    public Serializer getEntitySerializer() {
        return sqlCodegenModule.get(Serializer.class);
    }

    @Override
    public SerializerConfig getSerializerConfig(EntityType entityType) {
        return SimpleSerializerConfig.DEFAULT;
    }
}
