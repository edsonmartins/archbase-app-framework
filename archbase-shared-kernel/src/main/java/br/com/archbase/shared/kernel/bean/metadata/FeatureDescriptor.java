package br.com.archbase.shared.kernel.bean.metadata;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A classe FeatureDescriptor é a classe básica comum para PropertyDescriptor,
 * EventSetDescriptor e MethodDescriptor, etc.
 * <p>
 * Suporta algumas informações comuns que podem ser definidas e recuperadas para qualquer um dos
 * os descritores de introspecção.
 * <p>
 * Além disso, fornece um mecanismo de extensão para que
 * Os pares de atributo / valor podem ser associados a um recurso de design.
 */

public class FeatureDescriptor {

    private boolean expert;
    private boolean hidden;
    private boolean preferred;
    private String shortDescription;
    private String name;
    private String displayName;
    private HashMap<String, Object> table;
    private Reference<?> classRef;

    /**
     * Constrói um <code> FeatureDescriptor </code>.
     */
    public FeatureDescriptor() {
    }

    /**
     * Construtor de pacote privado,
     * Mesclar informações de dois FeatureDescriptors.
     * Os sinalizadores ocultos e de especialista mesclados são formados por or-ing os valores.
     * No caso de outros conflitos, o segundo argumento (y) é
     * dada prioridade sobre o primeiro argumento (x).
     *
     * @param x O primeiro MethodDescriptor (prioridade mais baixa)
     * @param y O segundo MethodDescriptor (de maior prioridade)
     */
    FeatureDescriptor(FeatureDescriptor x, FeatureDescriptor y) {
        expert = x.expert || y.expert;
        hidden = x.hidden || y.hidden;
        preferred = x.preferred || y.preferred;
        name = y.name;
        shortDescription = x.shortDescription;
        if (y.shortDescription != null) {
            shortDescription = y.shortDescription;
        }
        displayName = x.displayName;
        if (y.displayName != null) {
            displayName = y.displayName;
        }
        classRef = x.classRef;
        if (y.classRef != null) {
            classRef = y.classRef;
        }
        addTable(x.table);
        addTable(y.table);
    }

    /**
     * Construtor dup privado de pacote
     * Isso deve isolar o novo objeto de quaisquer alterações no objeto antigo.
     */
    FeatureDescriptor(FeatureDescriptor old) {
        expert = old.expert;
        hidden = old.hidden;
        preferred = old.preferred;
        name = old.name;
        shortDescription = old.shortDescription;
        displayName = old.displayName;
        classRef = old.classRef;

        addTable(old.table);
    }

    /**
     * Crie um invólucro de referência para o objeto.
     *
     * @param obj  objeto que será embrulhado
     * @param soft true se um SoftReference deve ser criado; caso contrário, macio
     * @return a Reference ou null se obj for null.
     */
    static Reference<Object> createReference(Object obj, boolean soft) {
        Reference<Object> ref = null;
        if (obj != null) {
            if (soft) {
                ref = new SoftReference<>(obj);
            } else {
                ref = new WeakReference<>(obj);
            }
        }
        return ref;
    }

    // Método de conveniência que cria uma WeakReference.
    static Reference<Object> createReference(Object obj) {
        return createReference(obj, false);
    }

    /**
     * Retorna um objeto de um wrapper de Referência.
     *
     * @return the Object em um wrapper ou null.
     */
    static Object getObject(Reference<?> ref) {
        return (ref == null) ? null : ref.get();
    }

    static String capitalize(String s) {
        return NameGenerator.capitalize(s);
    }

    /**
     * Obtém o nome programático deste recurso.
     *
     * @return O nome programático da propriedade / método / evento
     */
    public String getName() {
        return name;
    }

    /**
     * Define o nome programático deste recurso.
     *
     * @param name O nome programático da propriedade / método / evento
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtém o nome de exibição localizado deste recurso.
     *
     * @return O nome de exibição localizado para a propriedade / método / evento.
     * O padrão é o mesmo que o nome programático de getName.
     */
    public String getDisplayName() {
        if (displayName == null) {
            return getName();
        }
        return displayName;
    }

    /**
     * Define o nome de exibição localizado deste recurso.
     *
     * @param displayName O nome de exibição localizado para o
     *                    propriedade / método / evento.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * O sinalizador "especialista" é usado para distinguir entre os recursos que são
     * destinado a usuários experientes daqueles que se destinam a usuários normais.
     *
     * @return True se este recurso for destinado ao uso apenas por especialistas.
     */
    public boolean isExpert() {
        return expert;
    }

    /**
     * O sinalizador "especialista" é usado para distinguir entre recursos que são
     * destinado a usuários experientes daqueles que se destinam a usuários normais.
     *
     * @param expert Verdadeiro se este recurso for destinado ao uso apenas por especialistas.
     */
    public void setExpert(boolean expert) {
        this.expert = expert;
    }

    /**
     * O sinalizador "oculto" é usado para identificar recursos destinados apenas
     * para uso de ferramenta, e que não deve ser exposto a humanos.
     *
     * @return True se este recurso deve ser escondido de usuários humanos.
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * O sinalizador "oculto" é usado para identificar recursos destinados apenas
     * para uso de ferramenta, e que não deve ser exposto a humanos.
     *
     * @param hidden Verdadeiro se este recurso deve ser escondido de usuários humanos.
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * O sinalizador "preferencial" é usado para identificar recursos que são particularmente
     * importante para apresentações a humanos.
     *
     * @return True se este recurso deve ser mostrado preferencialmente para humanos
     * Comercial.
     */
    public boolean isPreferred() {
        return preferred;
    }

    /**
     * O sinalizador "preferencial" é usado para identificar recursos que são particularmente
     * importante para apresentações a humanos.
     *
     * @param preferred Verdadeiro se este recurso deve ser mostrado preferencialmente
     *                  para usuários humanos.
     */
    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    /**
     * Obtém uma breve descrição deste recurso.
     *
     * @return Uma breve descrição localizada associada a este
     * propriedade / método / evento. O padrão é o nome de exibição.
     */
    public String getShortDescription() {
        if (shortDescription == null) {
            return getDisplayName();
        }
        return shortDescription;
    }

    /**
     * Você pode associar uma string descritiva curta a um recurso. Normalmente
     * essas strings descritivas devem ter menos de cerca de 40 caracteres.
     *
     * @param text Uma breve descrição (localizada) a ser associada
     *             esta propriedade / método / evento.
     */
    public void setShortDescription(String text) {
        shortDescription = text;
    }

    /**
     * Associe um atributo nomeado a este recurso.
     *
     * @param attributeName O nome independente da localidade do atributo
     * @param value         O valor que.
     */
    public void setValue(String attributeName, Object value) {
        if (table == null) {
            table = new HashMap<>();
        }
        table.put(attributeName, value);
    }

    /**
     * Recupere um atributo nomeado com este recurso.
     *
     * @param attributeName O nome independente da localidade do atributo
     * @return O valor do atributo. Pode ser nulo se
     * o atributo é desconhecido.
     */
    public Object getValue(String attributeName) {
        if (table == null) {
            return null;
        }
        return table.get(attributeName);
    }

    /**
     * Obtém uma enumeração dos nomes independentes de local deste
     * característica.
     *
     * @return Uma enumeração dos nomes independentes de localidade de qualquer
     * atributos que foram registrados com setValue.
     */
    public Iterator<String> attributeNames() {
        if (table == null) {
            table = new HashMap<>();
        }
        return table.keySet().iterator();
    }

    private void addTable(HashMap<?, ?> t) {
        if (t == null) {
            return;
        }
        Iterator<?> keys = t.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = t.get(key);
            setValue(key, value);
        }
    }

    Class<?> getClass0() {
        return (Class<?>) getObject(classRef);
    }

    void setClass0(Class<?> cls) {
        classRef = createReference(cls);
    }


}
