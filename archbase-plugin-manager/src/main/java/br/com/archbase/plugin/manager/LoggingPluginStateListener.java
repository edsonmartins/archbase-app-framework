package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * É uma implementação de {@link PluginStateListener} que grava todos os eventos no logger (nível DEBUG).
 * Este listener é adicionado automaticamente por {@link DefaultArchbasePluginManager} para o modo {@code dev}.
 */
public class LoggingPluginStateListener implements PluginStateListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingPluginStateListener.class);

    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        log.debug("O estado do archbasePlugin '{}' mudou de '{}' para '{}'", event.getPlugin().getPluginId(),
                event.getOldState(), event.getPluginState());
    }

}
