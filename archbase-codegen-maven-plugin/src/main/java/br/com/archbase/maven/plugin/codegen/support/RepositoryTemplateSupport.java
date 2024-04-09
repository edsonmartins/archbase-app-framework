package br.com.archbase.maven.plugin.codegen.support;

import br.com.archbase.maven.plugin.codegen.provider.AbstractTemplateProvider;
import br.com.archbase.maven.plugin.codegen.support.maker.RepositoryStructure;
import br.com.archbase.maven.plugin.codegen.util.CustomResourceLoader;
import br.com.archbase.maven.plugin.codegen.util.Tuple;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;

import java.util.Set;

public class RepositoryTemplateSupport extends AbstractTemplateProvider {

    private CustomResourceLoader loader;
    private Set<String> additionalExtends;
    private String apiVersion;
    private String revisionNumberClass;

    public RepositoryTemplateSupport(AnnotationAttributes attributes, Set<String> additionalExtends, String apiVersion, String revisionNumberClass) {
        super(attributes);
        this.additionalExtends = additionalExtends;
        this.apiVersion = apiVersion;
        this.revisionNumberClass = revisionNumberClass;
    }

    public RepositoryTemplateSupport(CustomResourceLoader loader, Set<String> additionalExtends, String apiVersion, String revisionNumberClass) {
        super(loader);
        this.loader = loader;
        this.additionalExtends = additionalExtends;
        this.apiVersion = apiVersion;
        this.revisionNumberClass = revisionNumberClass;
    }

    @Override
    protected Tuple<String, Integer> getContentFromTemplate(String repositoryPackage, String simpleClassName, String postfix, BeanDefinition beanDefinition, String additionalPackage) {
        return new RepositoryStructure(repositoryPackage, simpleClassName, beanDefinition.getBeanClassName(), postfix, loader, additionalExtends, apiVersion, revisionNumberClass).build();
    }

    @Override
    protected String getExcludeClasses() {
        return "excludeRepositoriesClasses";
    }

    @Override
    protected String getPostfix() {
        return "repositoryPostfix";
    }

}
