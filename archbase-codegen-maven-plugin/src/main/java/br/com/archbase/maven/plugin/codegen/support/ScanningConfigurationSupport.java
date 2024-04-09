package br.com.archbase.maven.plugin.codegen.support;

import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.maven.plugin.codegen.annotation.ArchbaseDataGenerate;
import br.com.archbase.maven.plugin.codegen.annotation.ArchbaseNoDataGenerate;
import br.com.archbase.maven.plugin.codegen.provider.ClassPathScanningProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;


public class ScanningConfigurationSupport {

    private final Environment environment;
    private final AnnotationAttributes attributes;
    private final AnnotationMetadata annotationMetadata;
    private final String[] entityPackage;
    private final boolean onlyAnnotations;

    public ScanningConfigurationSupport(AnnotationMetadata annotationMetadata, AnnotationAttributes attributes, Environment environment) {
        Assert.notNull(environment, "Environment não deve ser nulo!");
        Assert.notNull(environment, "AnnotationMetadata não deve ser nulo!");
        this.environment = environment;
        this.attributes = attributes;
        this.annotationMetadata = annotationMetadata;
        this.entityPackage = this.attributes.getStringArray("entityPackage");
        this.onlyAnnotations = this.attributes.getBoolean("onlyAnnotations");
    }

    public ScanningConfigurationSupport(String[] entityPackage, boolean onlyAnnotations) {
        this.entityPackage = entityPackage;
        this.onlyAnnotations = onlyAnnotations;
        this.environment = null;
        this.annotationMetadata = null;
        this.attributes = null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Iterable<String> getBasePackages() {

        if (entityPackage.length == 0) {
            String className = this.annotationMetadata.getClassName();
            return Collections.singleton(ClassUtils.getPackageName(className));
        } else {
            HashSet packages = new HashSet();
            packages.addAll(Arrays.asList(entityPackage));

            return packages;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Collection<BeanDefinition> getCandidates(ResourceLoader resourceLoader) {
        if (this.getBasePackages() == null) {
            return Collections.emptyList();
        }

        ClassPathScanningProvider scanner = new ClassPathScanningProvider();
        scanner.setResourceLoader(resourceLoader);
        if (environment != null) {
            scanner.setEnvironment(this.environment);
        }

        scanner.setIncludeAnnotation(ArchbaseDataGenerate.class);
        scanner.setExcludeAnnotation(ArchbaseNoDataGenerate.class);
        if (!onlyAnnotations) {
            scanner.setIncludeAnnotation(DomainEntity.class);
        }

        Iterator filterPackages = this.getBasePackages().iterator();

        HashSet candidates = new HashSet();

        while (filterPackages.hasNext()) {
            String basePackage = (String) filterPackages.next();
            Set candidate = scanner.findCandidateComponents(basePackage);
            candidates.addAll(candidate);
        }

        return candidates;
    }
}

