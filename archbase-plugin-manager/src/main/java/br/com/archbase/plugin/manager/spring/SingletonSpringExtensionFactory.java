package br.com.archbase.plugin.manager.spring;


import br.com.archbase.plugin.manager.ArchbasePluginManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Um {@link SpringExtensionFactory} que sempre retorna uma instância específica.
 * Opcional, você pode especificar as classes de extensão para as quais deseja singletons.
 */
public class SingletonSpringExtensionFactory extends SpringExtensionFactory {

    private final List<String> extensionClassNames;

    private Map<String, Object> cache;

    public SingletonSpringExtensionFactory(ArchbasePluginManager archbasePluginManager) {
        this(archbasePluginManager, true);
    }

    public SingletonSpringExtensionFactory(ArchbasePluginManager archbasePluginManager, String... extensionClassNames) {
        this(archbasePluginManager, true, extensionClassNames);
    }

    public SingletonSpringExtensionFactory(ArchbasePluginManager archbasePluginManager, boolean autowire, String... extensionClassNames) {
        super(archbasePluginManager, autowire);

        this.extensionClassNames = Arrays.asList(extensionClassNames);

        cache = new HashMap<>(); // simple cache implementation
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> extensionClass) {
        String extensionClassName = extensionClass.getName();
        if (cache.containsKey(extensionClassName)) {
            return (T) cache.get(extensionClassName);
        }

        T extension = super.create(extensionClass);
        if (extensionClassNames.isEmpty() || extensionClassNames.contains(extensionClassName)) {
            cache.put(extensionClassName, extension);
        }

        return extension;
    }

}
