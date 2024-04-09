package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;


public class CompoundArchbasePluginLoader implements ArchbasePluginLoader {

    private static final Logger log = LoggerFactory.getLogger(CompoundArchbasePluginLoader.class);

    private List<ArchbasePluginLoader> loaders = new ArrayList<>();

    public CompoundArchbasePluginLoader add(ArchbasePluginLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("nulo não permitido");
        }

        loaders.add(loader);

        return this;
    }

    /**
     * Adicione um {@link ArchbasePluginLoader} apenas se a {@code condição} for satisfeita.
     *
     * @param loader
     * @param condition
     * @return
     */
    public CompoundArchbasePluginLoader add(ArchbasePluginLoader loader, BooleanSupplier condition) {
        if (condition.getAsBoolean()) {
            return add(loader);
        }

        return this;
    }

    public int size() {
        return loaders.size();
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        for (ArchbasePluginLoader loader : loaders) {
            if (loader.isApplicable(pluginPath)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
        for (ArchbasePluginLoader loader : loaders) {
            if (loader.isApplicable(pluginPath)) {
                log.debug("'{}' é aplicável para archbasePlugin '{}'", loader, pluginPath);
                try {
                    ClassLoader classLoader = loader.loadPlugin(pluginPath, pluginDescriptor);
                    if (classLoader != null) {
                        return classLoader;
                    }
                } catch (Exception e) {
                    // registre a exceção e continue com o próximo carregador
                    log.error(e.getMessage()); // ?!
                }
            } else {
                log.debug("'{}' não é aplicável para archbasePlugin '{}'", loader, pluginPath);
            }
        }

        throw new PluginLoaderException("Nenhum ArchbasePluginLoader para o archbasePlugin '" + pluginPath + "' e descritor '" + pluginDescriptor + "'");
    }

}
