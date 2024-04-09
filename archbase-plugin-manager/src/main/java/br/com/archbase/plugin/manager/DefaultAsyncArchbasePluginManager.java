package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Uma extensão de {@link DefaultArchbasePluginManager} que suporta métodos assíncronos (@ {AsyncPluginManager}).
 *
 */
public class DefaultAsyncArchbasePluginManager extends DefaultArchbasePluginManager implements AsyncPluginManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultAsyncArchbasePluginManager.class);

    @Override
    public CompletionStage<Void> loadPluginsAsync() {
        Path pluginsRoot = getPluginsRoot();
        PluginRepository pluginRepository = getPluginRepository();

        log.debug("Buscando plugins em '{}'", pluginsRoot);
        // verificar a raiz dos plug-ins
        if (Files.notExists(pluginsRoot) || !Files.isDirectory(pluginsRoot)) {
            log.warn("Sem '{}' root", pluginsRoot);
            return CompletableFuture.completedFuture(null);
        }

        // obtém todos os caminhos de plug-ins do repositório
        List<Path> pluginsPaths = pluginRepository.getPluginsPaths();

        // verifique se não há plug-ins
        if (pluginsPaths.isEmpty()) {
            log.info("Sem plugins");
            return CompletableFuture.completedFuture(null);
        }

        log.debug("Encontrados {} possíveis plug-ins: {}", pluginsPaths.size(), pluginsPaths);

        // carrega plugins de caminhos de plugin
        CompletableFuture<Void> feature = CompletableFuture.allOf(pluginsPaths.stream()
            .map(this::loadPluginFromPathAsync)
            .filter(Objects::nonNull)
            .toArray(CompletableFuture[]::new));

        // resolver plugins
        feature.thenRun(() -> {
            try {
                resolvePlugins();
            } catch (PluginRuntimeException e) {
                log.error(e.getMessage(), e);
            }
        });

        return feature;
    }

    @Override
    public CompletionStage<Void> startPluginsAsync() {
        return CompletableFuture.allOf(getResolvedPlugins().stream()
            .map(this::startPluginAsync)
            .toArray(CompletableFuture[]::new));
    }

    protected CompletionStage<PluginWrapper> loadPluginFromPathAsync(Path pluginPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadPluginFromPath(pluginPath);
            } catch (PluginRuntimeException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        });
    }

    protected CompletionStage<Void> startPluginAsync(PluginWrapper pluginWrapper) {
        return CompletableFuture.runAsync(() -> {
            PluginState pluginState = pluginWrapper.getPluginState();
            if ((PluginState.DISABLED != pluginState) && (PluginState.STARTED != pluginState)) {
                try {
                    log.info("Iniciando plugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
                    pluginWrapper.getPlugin().start();
                    pluginWrapper.setPluginState(PluginState.STARTED);
                    getStartedPlugins().add(pluginWrapper);

                    firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

}
