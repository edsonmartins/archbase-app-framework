package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class CompoundPluginDescriptorFinder implements PluginDescriptorFinder {

    private static final Logger log = LoggerFactory.getLogger(CompoundPluginDescriptorFinder.class);

    private List<PluginDescriptorFinder> finders = new ArrayList<>();

    public CompoundPluginDescriptorFinder add(PluginDescriptorFinder finder) {
        if (finder == null) {
            throw new IllegalArgumentException("nulo não permitido");
        }

        finders.add(finder);

        return this;
    }

    public int size() {
        return finders.size();
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        for (PluginDescriptorFinder finder : finders) {
            if (finder.isApplicable(pluginPath)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public PluginDescriptor find(Path pluginPath) {
        for (PluginDescriptorFinder finder : finders) {
            if (finder.isApplicable(pluginPath)) {
                log.debug("'{}' é aplicável para archbasePlugin '{}'", finder, pluginPath);
                try {
                    PluginDescriptor pluginDescriptor = finder.find(pluginPath);
                    if (pluginDescriptor != null) {
                        return pluginDescriptor;
                    }
                } catch (Exception e) {
                    if (finders.indexOf(finder) == finders.size() - 1) {
                        // é o último localizador
                        log.error(e.getMessage(), e);
                    } else {
                        // registre a exceção e continue com o próximo localizador
                        log.debug(e.getMessage());
                        log.debug("Tente continuar com o próximo localizador");
                    }
                }
            } else {
                log.debug("'{}' is not applicable for archbasePlugin '{}'", finder, pluginPath);
            }
        }

        throw new PluginRuntimeException("Nenhum PluginDescriptorFinder para o archbasePlugin '{}'", pluginPath);
    }

}
