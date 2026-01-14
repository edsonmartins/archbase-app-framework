/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.archbase.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * Desserializador para Value Objects com um único campo.
 * <p>
 * Permite criar instâncias de Value Objects a partir do valor direto,
 * sem exigir um wrapper JSON.
 * </p>
 * <p><b>Exemplo:</b></p>
 * <pre>
 * JSON: {"cpf": "123.456.789-09"}
 * Desserializado para: new CPF("123.456.789-09")
 * </pre>
 */
public class SingleValueObjectDeserializer extends JsonDeserializer<Object> {

    private final Class<?> targetType;
    private Constructor<?> singleArgConstructor;

    /**
     * Cria um novo desserializador para Value Objects.
     *
     * @param targetType a classe do Value Object
     */
    public SingleValueObjectDeserializer(Class<?> targetType) {
        this.targetType = targetType;
        this.singleArgConstructor = findSingleArgConstructor(targetType);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object value = p.getValueAsString();

        if (value == null) {
            // Tenta obter como objeto
            value = p.getEmbeddedObject();
        }

        if (value == null) {
            return null;
        }

        try {
            return createInstance(value);
        } catch (Exception e) {
            throw new JsonMappingException(p, "Failed to deserialize ValueObject: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma instância do Value Object usando o valor.
     *
     * @param value o valor do campo
     * @return a instância do Value Object
     */
    private Object createInstance(Object value) throws Exception {
        // Tenta usar o construtor com um argumento
        if (singleArgConstructor != null) {
            Object convertedValue = convertValue(value, singleArgConstructor.getParameterTypes()[0]);
            return singleArgConstructor.newInstance(convertedValue);
        }

        // Fallback: tenta encontrar um método estático "of" ou "from"
        try {
            return createViaStaticMethod(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Cannot deserialize " + targetType.getName() + ". " +
                "Add a single-argument constructor or a static factory method 'of()' or 'from()'.");
        }
    }

    /**
     * Encontra um construtor com um único argumento.
     */
    private Constructor<?> findSingleArgConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 1) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    /**
     * Tenta criar via método estático "of" ou "from".
     */
    private Object createViaStaticMethod(Object value) throws Exception {
        Class<?>[] paramTypes = new Class<?>[]{value.getClass()};

        try {
            // Tenta "of"
            return targetType.getMethod("of", paramTypes).invoke(null, value);
        } catch (NoSuchMethodException e) {
            // Tenta "from"
            try {
                return targetType.getMethod("from", paramTypes).invoke(null, value);
            } catch (NoSuchMethodException ex) {
                // Tenta "valueOf"
                return targetType.getMethod("valueOf", paramTypes).invoke(null, value);
            }
        }
    }

    /**
     * Converte o valor para o tipo esperado pelo construtor.
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // Conversão String para tipos primitivos/wrappers
        if (value instanceof String) {
            String strValue = (String) value;

            if (targetType == String.class) {
                return strValue;
            }

            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(strValue);
            }

            if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(strValue);
            }

            if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(strValue);
            }

            if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(strValue);
            }

            if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(strValue);
            }
        }

        return value;
    }
}
