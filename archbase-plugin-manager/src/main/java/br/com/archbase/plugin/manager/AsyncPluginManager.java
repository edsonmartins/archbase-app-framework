package br.com.archbase.plugin.manager;

import java.util.concurrent.CompletionStage;

/**
 * Um {@link ArchbasePluginManager} que oferece suporte à operação assíncrona com os métodos que terminam em {@code Async}.
 *
 */
public interface AsyncPluginManager extends ArchbasePluginManager {

    CompletionStage<Void> loadPluginsAsync();

    CompletionStage<Void> startPluginsAsync();

}
