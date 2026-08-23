package br.com.archbase.starter.core.auto.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Registrar que configura automaticamente component scan, entity scan e repository scan
 * para packages adicionais definidos via propriedades.
 *
 * Propriedades suportadas:
 * - archbase.app.component.scan: packages para component scan (separados por vírgula)
 * - archbase.app.jpa.entities: packages para entity scan (separados por vírgula)
 * - archbase.app.jpa.repositories: packages para repository scan (separados por vírgula)
 *
 * Compatible with Spring Boot 3.5.3+
 */
public class ArchbaseAdditionalScanConfigurer implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseAdditionalScanConfigurer.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registerComponentScan(registry);
        registerEntityScan(registry);
        registerRepositoryScan(registry);
    }

    private void registerComponentScan(BeanDefinitionRegistry registry) {
        String componentScanProperty = environment.getProperty("archbase.app.component.scan");
        if (!StringUtils.hasText(componentScanProperty)) {
            return;
        }

        String[] packages = StringUtils.commaDelimitedListToStringArray(componentScanProperty);
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);

        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (StringUtils.hasText(trimmed)) {
                int count = scanner.scan(trimmed);
                logger.info("Archbase: Component scan registrado automaticamente em '{}' ({} beans encontrados)", trimmed, count);
            }
        }
    }

    /**
     * Informa ao Spring Boot os pacotes de entidades — o mesmo que a anotação {@code @EntityScan} faz.
     *
     * <p>Antes isto usava um {@code ClassPathBeanDefinitionScanner} filtrando {@code @Entity},
     * {@code @MappedSuperclass} e {@code @Embeddable}. Esse scanner <b>registra beans</b>: cada
     * entidade da aplicação virava um singleton, instanciado pelo container na subida. Entidade JPA
     * não é bean — quem precisa saber dos pacotes é a fábrica de EntityManager, e ela lê
     * {@link EntityScanPackages}, não o registro de beans.
     *
     * <p>O engano ficou inócuo por muito tempo: sobravam instâncias vazias paradas no contexto. Com
     * o Spring 7 deixou de ser: {@code PersistenceAnnotationBeanPostProcessor.requiresDestruction}
     * consulta um mapa com o bean como chave, o que chama {@code hashCode()} na entidade recém
     * criada. Toda entidade com {@code equals}/{@code hashCode} sobre os próprios campos —
     * o padrão recomendado — derruba a subida com {@code NullPointerException}.
     */
    private void registerEntityScan(BeanDefinitionRegistry registry) {
        String entitiesProperty = environment.getProperty("archbase.app.jpa.entities");
        if (!StringUtils.hasText(entitiesProperty)) {
            return;
        }

        String[] packages = StringUtils.commaDelimitedListToStringArray(entitiesProperty);
        List<String> pacotes = new ArrayList<>();
        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (StringUtils.hasText(trimmed)) {
                pacotes.add(trimmed);
            }
        }

        if (!pacotes.isEmpty()) {
            EntityScanPackages.register(registry, pacotes);
            logger.info("Archbase: pacotes de entidades registrados para o entity scan: {}", pacotes);
        }
    }

    private void registerRepositoryScan(BeanDefinitionRegistry registry) {
        String repositoriesProperty = environment.getProperty("archbase.app.jpa.repositories");
        if (!StringUtils.hasText(repositoriesProperty)) {
            return;
        }

        // Repositórios JPA são registrados via ArchbaseDynamicJpaRepositoryConfigurer
        // que usa o mecanismo correto do Spring Data JPA
        String[] packages = StringUtils.commaDelimitedListToStringArray(repositoriesProperty);
        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (StringUtils.hasText(trimmed)) {
                logger.info("Archbase: Repository package '{}' será registrado via ArchbaseDynamicJpaRepositoryConfigurer", trimmed);
            }
        }
    }
}
