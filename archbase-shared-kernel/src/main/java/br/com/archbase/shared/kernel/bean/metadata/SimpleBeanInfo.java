package br.com.archbase.shared.kernel.bean.metadata;


/**
 * Esta é uma aula de suporte para tornar mais fácil para as pessoas fornecerem
 * Classes BeanInfo.
 * <p>
 * O padrão é fornecer informações "noop" e pode ser seletivamente
 * substituído para fornecer informações mais explícitas sobre os tópicos escolhidos.
 * Quando o introspector vir os valores "noop", ele aplicará valores baixos
 * nível de introspecção e padrões de design para analisar automaticamente
 * o bean de destino.
 */

public class SimpleBeanInfo implements BeanInfo {

    /**
     * Negar conhecimento sobre a classe e o personalizador do bean.
     * Você pode substituir isso se desejar fornecer informações explícitas.
     */
    public BeanDescriptor getBeanDescriptor() {
        return null;
    }

    /**
     * Negar conhecimento de propriedades. Você pode substituir isso
     * se você deseja fornecer informações de propriedade explícitas.
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        return new PropertyDescriptor[]{};
    }

    /**
     * Negar conhecimento de uma propriedade padrão. Você pode substituir isso
     * se você deseja definir uma propriedade padrão para o bean.
     */
    public int getDefaultPropertyIndex() {
        return -1;
    }

    /**
     * Negar conhecimento de conjuntos de eventos. Você pode substituir isso
     * se desejar fornecer informações explícitas do conjunto de eventos.
     */
    public EventSetDescriptor[] getEventSetDescriptors() {
        return new EventSetDescriptor[]{};
    }

    /**
     * Negar conhecimento de um evento padrão. Você pode substituir isso
     * se você deseja definir um evento padrão para o bean.
     */
    public int getDefaultEventIndex() {
        return -1;
    }

    /**
     * Negar conhecimento dos métodos. Você pode substituir isso
     * se você deseja fornecer informações de método explícitas.
     */
    public MethodDescriptor[] getMethodDescriptors() {
        return new MethodDescriptor[]{};
    }

    /**
     * Afirma que não há outros objetos BeanInfo relevantes. Você
     * pode substituir isso se você quiser (por exemplo) retornar um
     * BeanInfo para uma classe base.
     */
    public BeanInfo[] getAdditionalBeanInfo() {
        return new BeanInfo[]{};
    }


}
