package br.com.archbase.query.rls.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe responsável por localizar as classes anotadas com @DynamicFilter.
 */


@Service
public class ArchbaseRowLevelSecurityFilterEntityLocator {

    /**
     * Contém entidade com anotação DynamicFilter
     * e lista de classes de implementação de filtro
     */
    private final Map<Class<?>, Class<?>[]> entityMap;
    @Value("${archbase.dynamicfilter.entityBasePackage}")
    private String basePackageSearch;

    public ArchbaseRowLevelSecurityFilterEntityLocator() {
        this.entityMap = new HashMap<>();
    }

    @PostConstruct
    public void init() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DynamicFilter.class));
        for (BeanDefinition bd : scanner.findCandidateComponents(basePackageSearch)) {
            String className = bd.getBeanClassName();
            Class<?> annotedClass = Class.forName(className);
            DynamicFilter dynamicFilterAnnotation = annotedClass.getAnnotation(DynamicFilter.class);
            entityMap.put(annotedClass, dynamicFilterAnnotation.value());
        }
    }

    public String getBasePackageSearch() {
        return basePackageSearch;
    }

    public Class[] get(Class<?> key) {
        return entityMap.get(key);
    }
}
