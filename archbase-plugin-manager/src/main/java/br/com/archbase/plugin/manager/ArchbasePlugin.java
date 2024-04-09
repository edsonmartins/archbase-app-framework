package br.com.archbase.plugin.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Esta classe será estendida por todos os plug-ins e
 * servir como a classe comum entre um archbasePlugin e o aplicativo.
 */
public class ArchbasePlugin {

    /**
     * Disponibiliza serviço de registro para classes descendentes.
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Wrapper do archbasePlugin.
     */
    protected PluginWrapper wrapper;

    /**
     * Construtor a ser usado pelo gerenciador de archbasePlugin para instanciação do archbasePlugin.
     * Seus plug-ins devem fornecer ao construtor esta assinatura exata para
     * ser carregado com sucesso pelo gerenciador.
     */
    public ArchbasePlugin(final PluginWrapper wrapper) {
        if (wrapper == null) {
            throw new IllegalArgumentException("Wrapper não pode ser nulo");
        }

        this.wrapper = wrapper;
    }

    /**
     * Recupera o invólucro deste plug-in.
     */
    public final PluginWrapper getWrapper() {
        return wrapper;
    }

    /**
     * Este método é chamado pelo aplicativo quando o archbasePlugin é iniciado.
     * Consulte {@link ArchbasePluginManager#startPlugin(String)}.
     */
    public void start() {
        //
    }

    /**
     * Este método é chamado pelo aplicativo quando o plug-in é interrompido.
     * Consulte {@link ArchbasePluginManager#stopPlugin(String)}.
     */
    public void stop() {
        //
    }

    /**
     * Este método é chamado pelo aplicativo quando o archbasePlugin é excluído.
     * Consulte {@link ArchbasePluginManager#deletePlugin(String)}.
     */
    public void delete() {
        //
    }

}
