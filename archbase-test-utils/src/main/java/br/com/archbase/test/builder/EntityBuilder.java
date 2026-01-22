package br.com.archbase.test.builder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Builder genérico para criação de entidades em testes.
 * Usa reflexão para configurar campos de entidades.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * ClientEntity client = EntityBuilder.of(ClientEntity.class)
 *     .set("name", "João Silva")
 *     .set("email", "joao@example.com")
 *     .set("active", true)
 *     .build();
 * }
 * </pre>
 *
 * @param <T> Tipo da entidade
 */
public class EntityBuilder<T> {

    private final Class<T> entityClass;
    private final Map<String, Object> fieldValues = new HashMap<>();
    private final List<Consumer<T>> postBuildActions = new ArrayList<>();

    private EntityBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Cria um novo builder para a classe de entidade especificada.
     *
     * @param entityClass Classe da entidade
     * @param <T>         Tipo da entidade
     * @return Nova instância do EntityBuilder
     */
    public static <T> EntityBuilder<T> of(Class<T> entityClass) {
        return new EntityBuilder<>(entityClass);
    }

    /**
     * Define o valor de um campo da entidade.
     *
     * @param fieldName Nome do campo
     * @param value     Valor a ser atribuído
     * @return Este builder para encadeamento
     */
    public EntityBuilder<T> set(String fieldName, Object value) {
        fieldValues.put(fieldName, value);
        return this;
    }

    /**
     * Adiciona uma ação a ser executada após a construção da entidade.
     * Útil para configurar campos que requerem lógica complexa.
     *
     * @param action Ação a ser executada
     * @return Este builder para encadeamento
     */
    public EntityBuilder<T> with(Consumer<T> action) {
        postBuildActions.add(action);
        return this;
    }

    /**
     * Constrói a entidade com os valores configurados.
     *
     * @return Nova instância da entidade
     */
    public T build() {
        try {
            T instance = entityClass.getDeclaredConstructor().newInstance();
            applyFieldValues(instance);
            executePostBuildActions(instance);
            return instance;
        } catch (NoSuchMethodException e) {
            throw new EntityBuilderException(
                    "Classe " + entityClass.getName() + " não possui construtor padrão. " +
                            "Use EntityBuilder.of(Class, Supplier) para fornecer um factory method.", e);
        } catch (Exception e) {
            throw new EntityBuilderException("Erro ao construir instância de " + entityClass.getName(), e);
        }
    }

    /**
     * Constrói a entidade usando um factory method fornecido.
     *
     * @param factory Factory method que cria a instância
     * @return Nova instância da entidade
     */
    public T build(java.util.function.Supplier<T> factory) {
        T instance = factory.get();
        applyFieldValues(instance);
        executePostBuildActions(instance);
        return instance;
    }

    private void applyFieldValues(T instance) {
        fieldValues.forEach((fieldName, value) -> {
            try {
                Field field = findFieldInClass(instance.getClass(), fieldName);
                if (field == null) {
                    throw new EntityBuilderException(
                            "Campo '" + fieldName + "' não encontrado em " + instance.getClass().getName());
                }
                field.setAccessible(true);
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new EntityBuilderException(
                        "Erro ao definir campo '" + fieldName + "' em " + instance.getClass().getName(), e);
            }
        });
    }

    private Field findFieldInClass(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private void executePostBuildActions(T instance) {
        for (Consumer<T> action : postBuildActions) {
            action.accept(instance);
        }
    }

    /**
     * Cria um builder com valores padrão para campos comuns.
     * Gera valores automáticos para id, code, version, etc.
     *
     * @param entityClass Classe da entidade
     * @param <T>         Tipo da entidade
     * @return Nova instância do EntityBuilder com valores padrão
     */
    public static <T> EntityBuilder<T> withDefaults(Class<T> entityClass) {
        EntityBuilder<T> builder = new EntityBuilder<>(entityClass);

        // Tentar definir valores padrão para campos comuns
        try {
            setDefaultFieldValue(entityClass, "id", UUID.randomUUID(), builder);
            setDefaultFieldValue(entityClass, "code", "DEFAULT-" + System.currentTimeMillis(), builder);
            setDefaultFieldValue(entityClass, "version", 0L, builder);
            setDefaultFieldValue(entityClass, "active", true, builder);
        } catch (Exception e) {
            // Ignorar erros ao definir valores padrão
        }

        return builder;
    }

    private static <T> void setDefaultFieldValue(Class<T> entityClass, String fieldName,
                                                   Object value, EntityBuilder<T> builder) {
        try {
            if (findFieldInClassStatic(entityClass, fieldName) != null) {
                builder.fieldValues.put(fieldName, value);
            }
        } catch (Exception ignored) {
            // Campo não existe ou não é acessível
        }
    }

    private static Field findFieldInClassStatic(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Exceção lançada quando ocorre um erro ao construir a entidade.
     */
    public static class EntityBuilderException extends RuntimeException {

        public EntityBuilderException(String message) {
            super(message);
        }

        public EntityBuilderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
