package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * A implementação padrão para {@link ArchbasePluginFactory}.
 * Ele usa o método {@link Class#getConstructor}.
 */
public class DefaultArchbasePluginFactory implements ArchbasePluginFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultArchbasePluginFactory.class);

    /**
     * Cria uma instância de archbasePlugin. Se ocorrer um erro, esse erro será registrado e o método retornará nulo.
     *
     * @param pluginWrapper
     * @return
     */
    @Override
    public ArchbasePlugin create(final PluginWrapper pluginWrapper) {
        String pluginClassName = pluginWrapper.getDescriptor().getPluginClass();
        log.debug("Criar instância para archbasePlugin '{}'", pluginClassName);

        Class<?> pluginClass;
        try {
            pluginClass = pluginWrapper.getPluginClassLoader().loadClass(pluginClassName);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            return null;
        }

        // assim que tivermos a classe, podemos fazer algumas verificações para garantir
        // que é uma implementação válida de um archbasePlugin.
        int modifiers = pluginClass.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)
                || (!ArchbasePlugin.class.isAssignableFrom(pluginClass))) {
            log.error("A classe de archbasePlugin '{}' não é válida", pluginClassName);
            return null;
        }

        // cria a instância do archbasePlugin
        try {
            Constructor<?> constructor = pluginClass.getConstructor(PluginWrapper.class);
            return (ArchbasePlugin) constructor.newInstance(pluginWrapper);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

}
