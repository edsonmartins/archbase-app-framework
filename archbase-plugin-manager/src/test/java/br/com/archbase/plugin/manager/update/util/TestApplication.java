package br.com.archbase.plugin.manager.update.util;

import br.com.archbase.plugin.manager.ArchbasePluginManager;
import br.com.archbase.plugin.manager.DefaultArchbasePluginManager;
import br.com.archbase.plugin.manager.PluginRuntimeException;
import br.com.archbase.plugin.manager.update.PluginInfo;
import br.com.archbase.plugin.manager.update.UpdateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestApplication {

    private static final Logger log = LoggerFactory.getLogger(TestApplication.class);

    private final ArchbasePluginManager archbasePluginManager;
    private final UpdateManager updateManager;

    public TestApplication() {
        Path pluginsPath;
        try {
            pluginsPath = Files.createTempDirectory("plugins");
        } catch (IOException e) {
            throw new PluginRuntimeException("Falha ao criar diretório temporário de plug-ins", e);
        }

        archbasePluginManager = new DefaultArchbasePluginManager(pluginsPath);
        updateManager = new UpdateManager(archbasePluginManager);
    }

    public void start() {
        archbasePluginManager.loadPlugins();
    }

    public void update() {
        // >> manter o sistema atualizado <<
        boolean systemUpToDate = true;

        // verifique se há atualizações
        if (updateManager.hasUpdates()) {
            List<PluginInfo> updates = updateManager.getUpdates();
            log.debug("Encontrou {} atualizações", updates.size());
            for (PluginInfo plugin : updates) {
                log.debug("Atualização encontrada para o plugin '{}'", plugin.id);
                PluginInfo.PluginRelease lastRelease = updateManager.getLastPluginRelease(plugin.id);
                String lastVersion = lastRelease.version;
                String installedVersion = archbasePluginManager.getPlugin(plugin.id).getDescriptor().getVersion();
                log.debug("Atualize o plugin '{}' da versão {} para a versão {}", plugin.id, installedVersion, lastVersion);
                boolean updated = updateManager.updatePlugin(plugin.id, lastVersion);
                if (updated) {
                    log.debug("ArchbasePlugin atualizado '{}'", plugin.id);
                } else {
                    log.error("Não é possível atualizar o plugin '{}'", plugin.id);
                    systemUpToDate = false;
                }
            }
        } else {
            log.debug("Nenhuma atualização encontrada");
        }

        // check for available (new) plugins
        if (updateManager.hasAvailablePlugins()) {
            List<PluginInfo> availablePlugins = updateManager.getAvailablePlugins();
            log.debug("Foram encontrados {} plug-ins disponíveis", availablePlugins.size());
            for (PluginInfo plugin : availablePlugins) {
                log.debug("ArchbasePlugin disponível encontrado '{}'", plugin.id);
                PluginInfo.PluginRelease lastRelease = updateManager.getLastPluginRelease(plugin.id);
                String lastVersion = lastRelease.version;
                log.debug("Instale o plugin '{}' com a versão {}", plugin.id, lastVersion);
                boolean installed = updateManager.installPlugin(plugin.id, lastVersion);
                if (installed) {
                    log.debug("ArchbasePlugin instalado '{}'", plugin.id);
                } else {
                    log.error("Não é possível instalar o plugin '{}'", plugin.id);
                    systemUpToDate = false;
                }
            }
        } else {
            log.debug("Nenhum plug-in disponível encontrado");
        }

        if (systemUpToDate) {
            log.debug("Sistema atualizado");
        }
    }

    public ArchbasePluginManager getPluginManager() {
        return archbasePluginManager;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }
}
