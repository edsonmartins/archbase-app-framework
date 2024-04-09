package br.com.archbase.plugin.manager.spring;


import br.com.archbase.plugin.manager.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class ExtensionsInjector {

    private static final Logger log = LoggerFactory.getLogger(ExtensionsInjector.class);

    protected final SpringArchbasePluginManager springPluginManager;
    protected final AbstractAutowireCapableBeanFactory beanFactory;

    public ExtensionsInjector(SpringArchbasePluginManager springPluginManager, AbstractAutowireCapableBeanFactory beanFactory) {
        this.springPluginManager = springPluginManager;
        this.beanFactory = beanFactory;
    }

    public void injectExtensions() {
        // adicionar extensões do classpath (não plug-in)
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(null);
        for (String extensionClassName : extensionClassNames) {
            try {
                log.debug("Registre a extensão '{}' como bean", extensionClassName);
                Class<?> extensionClass = getClass().getClassLoader().loadClass(extensionClassName);
                registerExtension(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }

        // adiciona extensões para cada plugin iniciado
        List<PluginWrapper> startedPlugins = springPluginManager.getStartedPlugins();
        for (PluginWrapper plugin : startedPlugins) {
            log.debug("Registrando extensões do plugin '{}' como beans", plugin.getPluginId());
            extensionClassNames = springPluginManager.getExtensionClassNames(plugin.getPluginId());
            for (String extensionClassName : extensionClassNames) {
                try {
                    log.debug("Registre a extensão '{}' como bean", extensionClassName);
                    Class<?> extensionClass = plugin.getPluginClassLoader().loadClass(extensionClassName);
                    registerExtension(extensionClass);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Registre uma extensão como bean.
     * Extensão de registro de implementação atual como singleton usando {@code beanFactory.registerSingleton ()}.
     * A instância de extensão é criada usando {@code archbasePluginManager.getExtensionFactory (). Create (extensionClass)}.
     * O nome do bean é o nome da classe de extensão.
     * Substitua este método se desejar outra estratégia de registro.
     */
    protected void registerExtension(Class<?> extensionClass) {
        Map<String, ?> extensionBeanMap = springPluginManager.getApplicationContext().getBeansOfType(extensionClass);
        if (extensionBeanMap.isEmpty()) {
            Object extension = springPluginManager.getExtensionFactory().create(extensionClass);
            beanFactory.registerSingleton(extensionClass.getName(), extension);
        } else {
            log.debug("Registro do bean abortado! A extensão '{}' já existia como bean!", extensionClass.getName());
        }
    }

}
