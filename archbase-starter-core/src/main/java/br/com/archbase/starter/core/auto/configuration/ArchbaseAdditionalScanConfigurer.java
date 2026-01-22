package br.com.archbase.starter.core.auto.configuration;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * Registrar que loga packages adicionais configurados via propriedades
 * Compatible with Spring Boot 3.5.3+
 */
public class ArchbaseAdditionalScanConfigurer implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // Log dos packages adicionais configurados
        logAdditionalPackages();
        
        // Para uma implementação completa, seria necessário registrar dinamicamente
        // Por agora, vamos apenas informar ao usuário como adicionar os packages
        informUserAboutAdditionalConfiguration();
    }

    private void logAdditionalPackages() {
        String componentScan = environment.getProperty("archbase.app.component.scan");
        String entities = environment.getProperty("archbase.app.jpa.entities");
        String repositories = environment.getProperty("archbase.app.jpa.repositories");
        
        if (StringUtils.hasText(componentScan)) {
            System.out.println("Archbase: Component scan adicional detectado: " + componentScan);
        }
        if (StringUtils.hasText(entities)) {
            System.out.println("Archbase: Entity packages adicionais detectados: " + entities);
        }
        if (StringUtils.hasText(repositories)) {
            System.out.println("Archbase: Repository packages adicionais detectados: " + repositories);
        }
    }

    private void informUserAboutAdditionalConfiguration() {
        String componentScan = environment.getProperty("archbase.app.component.scan");
        String entities = environment.getProperty("archbase.app.jpa.entities");
        String repositories = environment.getProperty("archbase.app.jpa.repositories");
        
        if (StringUtils.hasText(componentScan) || StringUtils.hasText(entities) || StringUtils.hasText(repositories)) {
            System.out.println("================================================================================");
            System.out.println("ARCHBASE FRAMEWORK - CONFIGURAÇÃO ADICIONAL NECESSÁRIA");
            System.out.println("================================================================================");
            
            if (StringUtils.hasText(componentScan)) {
                System.out.println("Para ativar component scan em: " + componentScan);
                System.out.println("Adicione na sua classe @SpringBootApplication:");
                System.out.println("@ComponentScan(basePackages = {\"" + componentScan + "\"})");
                System.out.println();
            }
            
            if (StringUtils.hasText(entities)) {
                System.out.println("Para ativar entity scan em: " + entities);
                System.out.println("Adicione na sua classe de configuração:");
                System.out.println("@EntityScan(basePackages = {\"" + entities + "\"})");
                System.out.println();
            }
            
            if (StringUtils.hasText(repositories)) {
                System.out.println("Para ativar repository scan em: " + repositories);
                System.out.println("Adicione na sua classe de configuração:");
                System.out.println("@EnableJpaRepositories(basePackages = {\"" + repositories + "\"})");
                System.out.println();
            }
            
            System.out.println("Essas configurações são necessárias devido às mudanças no Spring Boot 3.5.3");
            System.out.println("================================================================================");
        }
    }
}