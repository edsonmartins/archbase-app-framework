package br.com.archbase.shared.kernel.bean.metadata;

import java.beans.PropertyChangeListener;

/**
 * Uma classe PropertyEditor fornece suporte para GUIs que desejam
 * permite aos usuários editar um valor de propriedade de um determinado tipo.
 * <p>
 * PropertyEditor suporta uma variedade de maneiras diferentes de
 * exibindo e atualizando valores de propriedade. A maioria dos PropertyEditors irão
 * só precisa oferecer suporte a um subconjunto das diferentes opções disponíveis em
 * esta API.
 * <p>
 * Simple PropertyEditors só podem suportar getAsText e setAsText
 * métodos e não precisam de suporte (digamos) paintValue ou getCustomEditor. Mais
 * tipos complexos podem não ser capazes de suportar getAsText e setAsText, mas irão
 * em vez disso, suporta paintValue e getCustomEditor.
 * <p>
 * Cada propertyEditor deve suportar um ou mais dos três simples
 * estilos de exibição. Assim, ele pode (1) suportar isPaintable ou (2)
 * ambos retornam um String não nulo [] de getTags () e retornam um não nulo
 * valor de getAsText ou (3) simplesmente retorna uma string não nula de
 * getAsText ().
 * <p>
 * Cada editor de propriedade deve suportar uma chamada em setValue quando o argumento
 * objeto é do tipo para o qual é o propertyEditor correspondente.
 * Além disso, cada editor de propriedade deve oferecer suporte a um editor personalizado,
 * ou suporte setAsText.
 * <p>
 * Cada PropertyEditor deve ter um construtor nulo.
 */

public interface PropertyEditor {

    /**
     * Obtém o valor da propriedade.
     *
     * @return O valor da propriedade. Tipos primitivos como "int" irão
     * ser agrupado como o tipo de objeto correspondente, como
     * "java.lang.Integer".
     */

    Object getValue();

    /**
     * Defina (ou altere) o objeto a ser editado. Tipos primitivos como
     * como "int" deve ser agrupado como o tipo de objeto correspondente, como
     * "java.lang.Integer".
     *
     * @param value O novo objeto de destino a ser editado. Observe que este
     *              objeto não deve ser modificado pelo PropertyEditor, em vez disso
     *              o PropertyEditor deve criar um novo objeto para conter qualquer
     *              valor modificado.
     */
    void setValue(Object value);

    // ----------------------------------------------------------------------

    /**
     * Determina se este editor de propriedade pode ser pintado.
     *
     * @return True se a classe respeitar o método paintValue.
     */

    boolean isPaintable();


    /**
     * Retorna um fragmento de código Java que pode ser usado para definir uma propriedade
     * para corresponder ao estado atual do editor. Este método é pretendido
     * para uso ao gerar código Java para refletir as alterações feitas por meio do
     * editor de propriedades.
     * <p>
     * O fragmento de código deve ser livre de contexto e deve ser um Java legal
     * expressão conforme especificado pelo JLS.
     * <p>
     * Especificamente, se a expressão representa um cálculo, todas as classes
     * e membros estáticos devem ser totalmente qualificados. Esta regra se aplica a
     * construtores, métodos estáticos e argumentos não primitivos.
     * <p>
     * Deve-se ter cuidado ao avaliar a expressão, pois ela pode lançar
     * exceções. Em particular, os geradores de código devem garantir que
     * o código irá compilar na presença de uma expressão que pode ser verificada
     * exceções.
     * <p>
     * Os resultados de exemplo são:
     * <ul>
     * <li> Expressão primitiva: <code> 2 </code>
     * <li> Construtor de classe: <code> new java.awt.Color (127,127,34) </code>
     * <li> Campo estático: <code> java.awt.Color.orange </code>
     * <li> Método estático: <code> jakarta.swing.Box.createRigidArea (novo
     * java.awt.Dimension (0, 5)) </code>
     * </ul>
     *
     * @return um fragmento de código Java que representa um inicializador para o
     * valor atual. Não deve conter ponto e vírgula
     * ('<code>; </code>') para finalizar a expressão.
     */
    String getJavaInitializationString();

    // ----------------------------------------------------------------------

    /**
     * Obtém o valor da propriedade como texto.
     *
     * @return O valor da propriedade como uma string editável por humanos.
     * <p>
     * Retorna nulo se o valor não pode ser expresso como um editável
     * corda.
     * <p>
     * Se um valor não nulo for retornado, o PropertyEditor deve
     * esteja preparado para analisar essa string de volta em setAsText ().
     */
    String getAsText();

    /**
     * Defina o valor da propriedade analisando uma determinada String. Pode aumentar
     * java.lang.IllegalArgumentException se o String for
     * mal formatado ou se este tipo de propriedade não pode ser expresso
     * como texto.
     *
     * @param text A string a ser analisada.
     */
    void setAsText(String text);

    // ----------------------------------------------------------------------

    /**
     * Se o valor da propriedade deve ser um de um conjunto de valores marcados conhecidos,
     * então este método deve retornar um array de tags. Isso pode
     * ser usado para representar (por exemplo) valores enum. Se um PropertyEditor
     * suporta tags, então deve suportar o uso de setAsText com
     * um valor de tag como uma forma de definir o valor e o uso de getAsText
     * para identificar o valor atual.
     *
     * @return Os valores de tag para esta propriedade. Pode ser nulo se este
     * propriedade não pode ser representada como um valor marcado.
     */
    String[] getTags();


    /**
     * Determina se este editor de propriedades suporta um editor personalizado.
     *
     * @return True se o propertyEditor puder fornecer um editor personalizado.
     */
    boolean supportsCustomEditor();


    /**
     * Registre um ouvinte para o evento PropertyChange. Quando um
     * PropertyEditor muda seu valor, ele deve disparar um PropertyChange
     * evento em todos os PropertyChangeListeners registrados, especificando o
     * valor nulo para o nome da propriedade e ele próprio como fonte.
     *
     * @param listener Um objeto a ser invocado quando um PropertyChange
     *                 evento é disparado.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Remova um ouvinte para o evento PropertyChange.
     *
     * @param listener O ouvinte PropertyChange a ser removido.
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

}
