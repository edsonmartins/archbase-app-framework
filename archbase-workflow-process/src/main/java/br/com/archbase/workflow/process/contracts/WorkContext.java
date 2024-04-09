package br.com.archbase.workflow.process.contracts;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contexto de execução do trabalho. Isso pode ser usado para passar parâmetros iniciais para o
 * fluxo de trabalho e compartilhamento de dados entre unidades de trabalho.
 *
 * <strong> Instâncias de contexto de trabalho são thread-safe. </strong>
 *
 * @author edsonmartins
 */
public class WorkContext {

    public static final String INITIAL_DATA = "initial_data";
    public static final String LAST_ERROR = "last_error";

    private final Map<String, Object> context = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public Object get(String key) {
        return context.get(key);
    }

    public boolean has(String key) {
        return context.containsKey(key);
    }

    public Set<Map.Entry<String, Object>> getEntrySet() {
        return context.entrySet();
    }

    @Override
    public String toString() {
        return "context=" + context + '}';
    }
}
