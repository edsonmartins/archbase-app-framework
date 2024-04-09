package br.com.archbase.plugin.manager.spring;

import br.com.archbase.plugin.manager.ArchbasePlugin;
import br.com.archbase.plugin.manager.PluginWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;


public abstract class SpringPlugin extends ArchbasePlugin {

    private ApplicationContext applicationContext;

    protected SpringPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public final ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            applicationContext = createApplicationContext();
        }

        return applicationContext;
    }

    @Override
    public void stop() {
        // fechar applicationContext
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).close();
        }
    }

    protected abstract ApplicationContext createApplicationContext();

}
