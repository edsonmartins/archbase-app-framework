package br.com.archbase.plugin.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A implementação padrão para {@link ExtensionFinder}.
 * É um {@code ExtensionFinder} composto.
 */
public class DefaultArchbaseExtensionFinder implements ExtensionFinder, PluginStateListener {

    protected ArchbasePluginManager archbasePluginManager;
    protected List<ExtensionFinder> finders = new ArrayList<>();

    public DefaultArchbaseExtensionFinder(ArchbasePluginManager archbasePluginManager) {
        this.archbasePluginManager = archbasePluginManager;

        add(new LegacyExtensionFinder(archbasePluginManager));
    }

    @Override
    public <T> List<ExtensionWrapper<T>> find(Class<T> type) {
        List<ExtensionWrapper<T>> extensions = new ArrayList<>();
        for (ExtensionFinder finder : finders) {
            extensions.addAll(finder.find(type));
        }

        return extensions;
    }

    @Override
    public <T> List<ExtensionWrapper<T>> find(Class<T> type, String pluginId) {
        List<ExtensionWrapper<T>> extensions = new ArrayList<>();
        for (ExtensionFinder finder : finders) {
            extensions.addAll(finder.find(type, pluginId));
        }

        return extensions;
    }

    @Override
    @SuppressWarnings("java:S3740")
    public List<ExtensionWrapper> find(String pluginId) {
        List<ExtensionWrapper> extensions = new ArrayList<>();
        for (ExtensionFinder finder : finders) {
            extensions.addAll(finder.find(pluginId));
        }

        return extensions;
    }

    @Override
    public Set<String> findClassNames(String pluginId) {
        Set<String> classNames = new HashSet<>();
        for (ExtensionFinder finder : finders) {
            classNames.addAll(finder.findClassNames(pluginId));
        }

        return classNames;
    }

    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        for (ExtensionFinder finder : finders) {
            if (finder instanceof PluginStateListener) {
                ((PluginStateListener) finder).pluginStateChanged(event);
            }
        }
    }

    public DefaultArchbaseExtensionFinder addServiceProviderExtensionFinder() {
        return add(new ServiceProviderExtensionFinder(archbasePluginManager));
    }

    public DefaultArchbaseExtensionFinder add(ExtensionFinder finder) {
        finders.add(finder);

        return this;
    }

}
