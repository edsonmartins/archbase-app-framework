package br.com.archbase.shared.kernel.bean.metadata;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Uma classe de utilitário que gera nomes exclusivos para instâncias de objetos.
 * O nome será uma concatenação do nome da classe não qualificada
 * e um número de instância.
 * <p>
 * Por exemplo, se a primeira instância do objeto jakarta.swing.JButton for passada para
 * <code> instanceName </code> então o identificador de string retornado será
 * &quot;JButton0&quot;.
 */
class NameGenerator {

    private Map<Object, Object> valueToName;
    private Map<Object, Object> nameToCount;

    public NameGenerator() {
        valueToName = new IdentityHashMap<>();
        nameToCount = new HashMap<>();
    }

    /**
     * Retorna o nome raiz da classe.
     */
    public static String unqualifiedClassName(Class<?> type) {
        if (type.isArray()) {
            return unqualifiedClassName(type.getComponentType()) + "Array";
        }
        String name = type.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * Retorna uma String que coloca em maiúscula a primeira letra da string.
     */
    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Limpa o cache de nomes. Deve ser chamado para perto do final de
     * o ciclo de codificação.
     */
    public void clear() {
        valueToName.clear();
        nameToCount.clear();
    }

    /**
     * Retorna uma string única que identifica a instância do objeto.
     * As invocações são armazenadas em cache de forma que, se um objeto foi anteriormente
     * passado para este método, o mesmo identificador é retornado.
     *
     * @param instance objeto usado para gerar string
     * @return uma string única que representa o objeto
     */
    public String instanceName(Object instance) {
        if (instance == null) {
            return "null";
        }
        if (instance instanceof Class) {
            return unqualifiedClassName((Class) instance);
        } else {
            String result = (String) valueToName.get(instance);
            if (result != null) {
                return result;
            }
            Class<?> type = instance.getClass();
            String className = unqualifiedClassName(type);

            Object size = nameToCount.get(className);
            int instanceNumber = (size == null) ? 0 : ((Integer) size).intValue() + 1;
            nameToCount.put(className, Integer.valueOf(instanceNumber));

            result = className + instanceNumber;
            valueToName.put(instance, result);
            return result;
        }
    }
}
