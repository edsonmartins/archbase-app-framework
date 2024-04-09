package br.com.archbase.shared.kernel.bean.metadata;

/**
 * Um implementador de bean que deseja fornecer informações explícitas sobre
 * seu bean pode fornecer uma classe BeanInfo que implementa este BeanInfo
 * interface e fornece informações explícitas sobre os métodos,
 * propriedades, eventos, etc, de seu feijão.
 * <p>
 * Um implementador de bean não precisa fornecer um conjunto completo de
 * em formação. Você pode escolher quais informações deseja fornecer
 * e o restante será obtido por análise automática usando baixo nível
 * reflexão dos métodos das classes de feijão e aplicação de design padrão
 * padrões.
 * <p>
 * Você tem a oportunidade de fornecer muitas e muitas informações diferentes como
 * parte das várias classes XyZDescriptor. Mas não entre em pânico, você só realmente
 * necessidade de fornecer as informações essenciais mínimas exigidas pelos vários
 * construtores.
 * <p>
 * Veja também a classe SimpleBeanInfo que fornece uma base "noop" conveniente
 * classe para classes BeanInfo, que você pode substituir para esses lugares específicos
 * onde você deseja retornar informações explícitas.
 * <p>
 * Para aprender sobre t_odo o comportamento de um bean, consulte a classe Introspector.
 */

public interface BeanInfo {

    /**
     * Obtém os beans <code> BeanDescriptor </code>.
     *
     * @return Um BeanDescriptor que fornece informações gerais sobre
     * o bean, como seu displayName, seu personalizador, etc. Pode
     * retorna nulo se a informação deve ser obtida automaticamente
     * análise.
     */
    BeanDescriptor getBeanDescriptor();

    /**
     * Obtém os beans <code> EventSetDescriptor </code> s.
     *
     * @return Uma matriz de EventSetDescriptors que descreve os tipos de
     * eventos disparados por este bean. Pode retornar nulo se a informação
     * deve ser obtido por análise automática.
     */
    EventSetDescriptor[] getEventSetDescriptors();

    /**
     * Um bean pode ter um evento "padrão" que é o evento que irá
     * geralmente usado por humanos ao usar o feijão.
     *
     * @return Índice de evento padrão na matriz EventSetDescriptor
     * retornado por getEventSetDescriptors.
     * <p>
     * Retorna -1 se não houver evento padrão.
     */
    int getDefaultEventIndex();

    /**
     * Obtém os beans <code> PropertyDescriptor </code> s.
     *
     * @return Uma matriz de PropertyDescriptors que descreve o editável
     * propriedades suportadas por este bean. Pode retornar nulo se o
     * as informações devem ser obtidas por análise automática.
     * <p>
     * Se uma propriedade for indexada, sua entrada na matriz de resultado será
     * pertence à subclasse IndexedPropertyDescriptor de
     * PropertyDescriptor. Um cliente de getPropertyDescriptors pode usar
     * "instanceof" para verificar se um determinado PropertyDescriptor é um
     * IndexedPropertyDescriptor.
     */
    PropertyDescriptor[] getPropertyDescriptors();

    /**
     * Um bean pode ter uma propriedade "padrão" que é a propriedade que irá
     * geralmente é escolhido inicialmente para atualização por humanos que são
     * personalizar o feijão.
     *
     * @return Índice da propriedade padrão na matriz PropertyDescriptor
     * retornado por getPropertyDescriptors.
     * <p>
     * Retorna -1 se não houver propriedade padrão.
     */
    int getDefaultPropertyIndex();

    /**
     * Obtém os beans <code> MethodDescriptor </code> s.
     *
     * @return Uma matriz de MethodDescriptors que descreve o externamente
     * métodos visíveis suportados por este bean. Pode retornar nulo se
     * as informações devem ser obtidas por análise automática.
     */
    MethodDescriptor[] getMethodDescriptors();

    /**
     * Este método permite que um objeto BeanInfo retorne uma coleção arbitrária
     * de outros objetos BeanInfo que fornecem informações adicionais sobre o
     * feijão atual.
     * <p>
     * Se houver conflitos ou sobreposições entre as informações fornecidas por
     * diferentes objetos BeanInfo, então o BeanInfo atual tem precedência
     * sobre os objetos getAdditionalBeanInfo e elementos posteriores na matriz
     * têm precedência sobre os anteriores.
     *
     * @return um array de objetos BeanInfo. Pode retornar nulo.
     */
    BeanInfo[] getAdditionalBeanInfo();

}
