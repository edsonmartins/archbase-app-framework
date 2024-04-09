package br.com.archbase.plugin.manager;

import java.util.*;

/**
 * Um {@link ExtensionFactory} que sempre retorna uma instância específica.
 * Opcional, você pode especificar as classes de extensão para as quais deseja singletons.
 */
public class SingletonArchbaseExtensionFactory extends DefaultArchbaseExtensionFactory {

    private final List<String> extensionClassNames;

    private Map<ClassLoader, Map<String, Object>> cache;

    public SingletonArchbaseExtensionFactory(String... extensionClassNames) {
        this.extensionClassNames = Arrays.asList(extensionClassNames);

        cache = new WeakHashMap<>(); // implementação de cache simples
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> extensionClass) {
        String extensionClassName = extensionClass.getName();
        ClassLoader extensionClassLoader = extensionClass.getClassLoader();

        if (!cache.containsKey(extensionClassLoader)) {
            cache.put(extensionClassLoader, new HashMap<>());
        }

        Map<String, Object> classLoaderBucket = cache.get(extensionClassLoader);

        if (classLoaderBucket.containsKey(extensionClassName)) {
            return (T) classLoaderBucket.get(extensionClassName);
        }

        T extension = super.create(extensionClass);
        if (extensionClassNames.isEmpty() || extensionClassNames.contains(extensionClassName)) {
            classLoaderBucket.put(extensionClassName, extension);
        }

        return extension;
    }

}
