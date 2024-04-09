package br.com.archbase.maven.plugin.codegen.support;

import br.com.archbase.maven.plugin.codegen.provider.AbstractTemplateProvider;
import br.com.archbase.maven.plugin.codegen.support.maker.ResourceStructure;
import br.com.archbase.maven.plugin.codegen.util.CustomResourceLoader;
import br.com.archbase.maven.plugin.codegen.util.GeneratorUtils;
import br.com.archbase.maven.plugin.codegen.util.Tuple;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;

import java.io.File;
import java.util.Arrays;


public class ResourceTemplateSupport extends AbstractTemplateProvider {

    private String servicePackage;
    private String servicePostfix;
    private CustomResourceLoader loader;
    private String apiVersion;
    private String revisionNumberClass;

    public ResourceTemplateSupport(CustomResourceLoader loader, AnnotationAttributes attributes, String servicePackage, String servicePostfix, String apiVersion, String revisionNumberClass) {
        super(attributes);
        this.servicePackage = servicePackage;
        this.servicePostfix = servicePostfix;
        this.loader = loader;
        this.apiVersion = apiVersion;
        this.revisionNumberClass = revisionNumberClass;
        this.findFilterRepositories();
    }

    public ResourceTemplateSupport(CustomResourceLoader customResourceLoader, String apiVersion, String revisionNumberClass) {
        super(customResourceLoader);
        this.loader = customResourceLoader;
        this.servicePackage = customResourceLoader.getServicePackage();
        this.servicePostfix = customResourceLoader.getServicePostfix();
        this.apiVersion = apiVersion;
        this.revisionNumberClass = revisionNumberClass;
        this.findFilterRepositories();
    }

    private void findFilterRepositories() {
        String repositoryPath = GeneratorUtils.getAbsolutePath() + servicePackage.replace(".", "/");
        File[] repositoryFiles = GeneratorUtils.getFileList(repositoryPath, servicePostfix);
        this.setIncludeFilter(Arrays.asList(repositoryFiles));
        this.setIncludeFilterPostfix(servicePostfix);
    }

    @Override
    protected Tuple<String, Integer> getContentFromTemplate(String mPackage, String simpleClassName, String postfix, BeanDefinition beanDefinition, String additionalPackage) {
        return new ResourceStructure(mPackage, simpleClassName, beanDefinition.getBeanClassName(), postfix, servicePackage, servicePostfix, loader, additionalPackage, apiVersion, revisionNumberClass).build();
    }

    @Override
    protected String getExcludeClasses() {
        return "excludeResourceClasses";
    }

    @Override
    protected String getPostfix() {
        return "resourcePostfix";
    }

}