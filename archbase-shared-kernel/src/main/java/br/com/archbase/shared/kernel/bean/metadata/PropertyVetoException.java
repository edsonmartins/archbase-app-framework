package br.com.archbase.shared.kernel.bean.metadata;

import java.beans.PropertyChangeEvent;

/**
 * Uma PropertyVetoException é lançada quando uma mudança proposta para um
 * propriedade representa um valor inaceitável.
 */

public class PropertyVetoException extends Exception {

    /**
     * Forneça uma breve descrição de serialVersionUID.
     * Especifique a finalidade deste campo.
     */
    private static final long serialVersionUID = -2206020012556077235L;
    /**
     * Um PropertyChangeEvent que descreve a alteração vetada.
     *
     * @serial
     */
    private final PropertyChangeEvent evt;

    /**
     * Constrói uma <code> PropertyVetoException </code> com um
     * mensagem detalhada.
     *
     * @param mess Mensagem descritiva
     * @param evt  Um PropertyChangeEvent que descreve a mudança vetada.
     */
    public PropertyVetoException(String mess, PropertyChangeEvent evt) {
        super(mess);
        this.evt = evt;
    }

    /**
     * Obtém o <code> PropertyChangeEvent </code> vetado.
     *
     * @return Um PropertyChangeEvent que descreve a alteração vetada.
     */
    public PropertyChangeEvent getPropertyChangeEvent() {
        return evt;
    }
}
