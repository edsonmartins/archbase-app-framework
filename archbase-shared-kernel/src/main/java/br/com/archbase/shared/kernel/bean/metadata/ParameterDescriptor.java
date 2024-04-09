package br.com.archbase.shared.kernel.bean.metadata;

public class ParameterDescriptor extends FeatureDescriptor {

    /**
     * Construtor público padrão.
     */
    public ParameterDescriptor() {
    }

    /**
     * Construtor dup privado do pacote.
     * Isso deve isolar o novo objeto de quaisquer alterações no objeto antigo.
     */
    ParameterDescriptor(ParameterDescriptor old) {
        super(old);
    }

}
