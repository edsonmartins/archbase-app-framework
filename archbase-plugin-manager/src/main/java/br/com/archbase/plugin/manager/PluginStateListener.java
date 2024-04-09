package br.com.archbase.plugin.manager;

import java.util.EventListener;

/**
 * PluginStateListener define a interface para um objeto que escuta as mudanças de estado do archbasePlugin.
 */
public interface PluginStateListener extends EventListener {

    /**
     * Chamado quando o estado de um archbasePlugin (por exemplo, DESATIVADO, INICIADO) é alterado.
     */
    void pluginStateChanged(PluginStateEvent event);

}
