package br.com.archbase.plugin.manager.spring;

import br.com.archbase.plugin.manager.DefaultArchbasePluginManager;
import br.com.archbase.plugin.manager.ExtensionFactory;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import java.nio.file.Path;

public class SpringArchbasePluginManager extends DefaultArchbasePluginManager implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public SpringArchbasePluginManager() {
    }

    public SpringArchbasePluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new SpringExtensionFactory(this);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Este método carrega, inicia plugins e injeta extensões no Spring
     */
    @PostConstruct
    public void init() {
        loadPlugins();
        startPlugins();

        AbstractAutowireCapableBeanFactory beanFactory = (AbstractAutowireCapableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        ExtensionsInjector extensionsInjector = new ExtensionsInjector(this, beanFactory);
        extensionsInjector.injectExtensions();
    }

}
