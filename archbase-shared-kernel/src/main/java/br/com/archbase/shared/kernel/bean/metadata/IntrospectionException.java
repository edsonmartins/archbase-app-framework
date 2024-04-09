package br.com.archbase.shared.kernel.bean.metadata;

public class IntrospectionException extends Exception {

    /**
     * Forneça uma breve descrição de serialVersionUID.
     * Especifique a finalidade deste campo.
     */
    private static final long serialVersionUID = 1365256381098719405L;

    /**
     * Constrói uma <code> IntrospectionException </code> com um
     * mensagem detalhada.
     *
     * @param mess Mensagem descritiva
     */
    public IntrospectionException(String mess) {
        super(mess);
    }
}
