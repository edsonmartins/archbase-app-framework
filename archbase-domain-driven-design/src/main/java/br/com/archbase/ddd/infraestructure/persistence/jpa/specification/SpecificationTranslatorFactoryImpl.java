package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Implementação Spring orientada a {@link SpecificationTranslatorFactory}.
 * <p>
 * Permite que os conversores sejam verificados a partir do classpath, usando uma base específica
 * pacote. Sempre que forem encontradas classes de conversor, ele tentará recuperar
 * um bean do contexto do aplicativo atual. No caso de nenhum bean correspondente
 * for encontrado, uma nova instância de conversor nula é criada.
 *
 * @author edsonmartins
 */
public class SpecificationTranslatorFactoryImpl implements SpecificationTranslatorFactory, ApplicationContextAware {
    private ApplicationContext applicationContext;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SpecificationTranslator createWithAnnotatedConverters(String basePackage) {
        SpecificationTranslator translator = createWithDefaultConverters();
        // Encontre todos os conversores anotados no pacote básico fornecido
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new IsRegisterableConverterFilter());
        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                // Registre cada instância de conversor encontrada
                Class<?> converterClass = Class.forName(bd.getBeanClassName());
                translator.registerConverter(findOrCreateConverter(converterClass));
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new IllegalStateException(e);
            }
        }
        return translator;
    }

    /**
     * Recupere o conversor do contexto do aplicativo ou construa uma instância {@code new}.
     *
     * @param converterClass o tipo do nosso conversor
     * @return uma instância de conversor nova ou existente
     */
    private SpecificationConverter<?, ?> findOrCreateConverter(Class<?> converterClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object instance = null;
        try {
            instance = applicationContext.getBean(converterClass);
        } catch (NoSuchBeanDefinitionException e) {
            instance = converterClass.getDeclaredConstructor().newInstance();
        }
        return (SpecificationConverter<?, ?>) instance;
    }

    // Corresponde aos conversores de especificação para predicado anotados
    private static class IsRegisterableConverterFilter implements TypeFilter {
        private static final TypeFilter CONVERTER_TYPE_FILTER = new AssignableTypeFilter(SpecificationConverter.class);
        private static final TypeFilter REGISTERED_ANNOTATION_FILTER = new AnnotationTypeFilter(Registered.class);

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
            boolean isConverterType = CONVERTER_TYPE_FILTER.match(metadataReader, metadataReaderFactory);
            boolean isAnnotatedAsRegistered = REGISTERED_ANNOTATION_FILTER.match(metadataReader, metadataReaderFactory);
            return isConverterType && isAnnotatedAsRegistered;
        }
    }

}
