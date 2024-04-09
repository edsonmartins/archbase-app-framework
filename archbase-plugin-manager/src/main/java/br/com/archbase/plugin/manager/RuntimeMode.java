package br.com.archbase.plugin.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;


public enum RuntimeMode {

    DEVELOPMENT("development", "dev"), // desenvolvimento
    DEPLOYMENT("deployment", "prod"); // desenvolvimento

    private static final Map<String, RuntimeMode> map = new HashMap<>();

    static {
        for (RuntimeMode mode : RuntimeMode.values()) {
            map.put(mode.name, mode);
            for (String alias : mode.aliases) {
                map.put(alias, mode);
            }
        }
    }

    private final String name;
    private final String[] aliases;

    RuntimeMode(final String name, final String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public static RuntimeMode byName(String name) {
        if (map.containsKey(name)) {
            return map.get(name);
        }

        throw new NoSuchElementException("Não foi possível encontrar o modo de runtime do Archbase com o nome '" + name + "'." +
                "Deve ser um valor de '" + map.keySet() + ".");
    }

    @Override
    public String toString() {
        return name;
    }

}
