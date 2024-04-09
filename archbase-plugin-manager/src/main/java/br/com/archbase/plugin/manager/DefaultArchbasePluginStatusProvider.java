package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A implementação padrão para {@link PluginStatusProvider}.
 * Os plug-ins ativados são lidos do arquivo {@code enabled.txt} e
 * os plug-ins desativados são lidos do arquivo {@code disabled.txt}.
 */
public class DefaultArchbasePluginStatusProvider implements PluginStatusProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultArchbasePluginStatusProvider.class);

    private final Path pluginsRoot;

    private List<String> enabledPlugins;
    private List<String> disabledPlugins;

    public DefaultArchbasePluginStatusProvider(Path pluginsRoot) {
        this.pluginsRoot = pluginsRoot;

        try {
            // create a list with archbasePlugin identifiers that should be only accepted by this manager (whitelist from plugins/enabled.txt file)
            enabledPlugins = FileUtils.readLines(getEnabledFilePath(), true);
            log.info("Plugins habilitados: {}", enabledPlugins);

            // create a list with archbasePlugin identifiers that should not be accepted by this manager (blacklist from plugins/disabled.txt file)
            disabledPlugins = FileUtils.readLines(getDisabledFilePath(), true);
            log.info("Plugins habilitados: {}", disabledPlugins);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static Path getEnabledFilePath(Path pluginsRoot) {
        return pluginsRoot.resolve("enabled.txt");
    }

    public static Path getDisabledFilePath(Path pluginsRoot) {
        return pluginsRoot.resolve("disabled.txt");
    }

    @Override
    public boolean isPluginDisabled(String pluginId) {
        if (disabledPlugins.contains(pluginId)) {
            return true;
        }

        return !enabledPlugins.isEmpty() && !enabledPlugins.contains(pluginId);
    }

    @Override
    public void disablePlugin(String pluginId) {
        if (isPluginDisabled(pluginId)) {
            // fazer nada
            return;
        }

        if (Files.exists(getEnabledFilePath())) {
            enabledPlugins.remove(pluginId);
            try {
                FileUtils.writeLines(enabledPlugins, getEnabledFilePath());
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        } else {
            disabledPlugins.add(pluginId);

            try {
                FileUtils.writeLines(disabledPlugins, getDisabledFilePath());
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        }
    }

    @Override
    public void enablePlugin(String pluginId) {
        if (!isPluginDisabled(pluginId)) {
            // fazer nada
            return;
        }

        if (Files.exists(getEnabledFilePath())) {
            enabledPlugins.add(pluginId);

            try {
                FileUtils.writeLines(enabledPlugins, getEnabledFilePath());
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        } else {
            disabledPlugins.remove(pluginId);

            try {
                FileUtils.writeLines(disabledPlugins, getDisabledFilePath());
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        }
    }

    public Path getEnabledFilePath() {
        return getEnabledFilePath(pluginsRoot);
    }

    public Path getDisabledFilePath() {
        return getDisabledFilePath(pluginsRoot);
    }

}
