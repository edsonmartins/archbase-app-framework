package br.com.archbase.maven.plugin.codegen.support;

import br.com.archbase.maven.plugin.codegen.provider.AbstractTemplateProvider;
import br.com.archbase.maven.plugin.codegen.support.maker.ServiceStructure;
import br.com.archbase.maven.plugin.codegen.util.CustomResourceLoader;
import br.com.archbase.maven.plugin.codegen.util.GeneratorUtils;
import br.com.archbase.maven.plugin.codegen.util.Tuple;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;

import java.io.File;
import java.util.Arrays;


public class ServiceTemplateSupport extends AbstractTemplateProvider {

    private String repositoryPackage;
    private String repositoryPostfix;
    private CustomResourceLoader loader;
    private String apiVersion;
    private String revisionNumberClass;


    public ServiceTemplateSupport(CustomResourceLoader loader, AnnotationAttributes attributes, String repositoryPackage, String repositoryPostfix, String apiVersion, String revisionNumberClass) {
        super(attributes);
        this.loader = loader;
        this.repositoryPackage = repositoryPackage;
        this.repositoryPostfix = repositoryPostfix;
        this.apiVersion = apiVersion;
        this.revisionNumberClass = revisionNumberClass;
        this.findFilterRepositories();
    }

    public ServiceTemplateSupport(CustomResourceLoader loader, String apiVersion, String revisionNumberClass) {
        super(loader);
        this.loader = loader;
        this.repositoryPackage = loader.getRepositoryPackage();
        this.repositoryPostfix = loader.getRepositoryPostfix();
        this.apiVersion = apiVersion;
        this.revisionNumberClass = revisionNumberClass;
        this.findFilterRepositories();
    }

    private void findFilterRepositories() {
        String repositoryPath = GeneratorUtils.getAbsolutePath() + repositoryPackage.replace(".", "/");
        File[] repositoryFiles = GeneratorUtils.getFileList(repositoryPath, repositoryPostfix);
        this.setIncludeFilter(Arrays.asList(repositoryFiles));
        this.setIncludeFilterPostfix(repositoryPostfix);
    }

    @Override
    protected Tuple<String, Integer> getContentFromTemplate(String mPackage, String simpleClassName, String postfix, BeanDefinition beanDefinition, String additionalPackage) {
        return new ServiceStructure(mPackage, simpleClassName, beanDefinition.getBeanClassName(), postfix, repositoryPackage, repositoryPostfix, loader, additionalPackage, apiVersion, revisionNumberClass).build();
    }

    @Override
    protected String getExcludeClasses() {
        return "excludeServiceClasses";
    }

    @Override
    protected String getPostfix() {
        return "servicePostfix";
    }

}