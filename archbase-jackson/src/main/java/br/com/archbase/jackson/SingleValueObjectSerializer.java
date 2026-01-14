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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Serializador para Value Objects com um único campo.
 * <p>
 * Serializa o Value Object como o valor direto de seu único campo,
 * sem criar um wrapper JSON.
 * </p>
 * <p><b>Exemplo:</b></p>
 * <pre>
 * Sem este serializador: {"cpf": {"valor": "123.456.789-09"}}
 * Com este serializador:  {"cpf": "123.456.789-09"}
 * </pre>
 */
public class SingleValueObjectSerializer extends JsonSerializer<Object> {

    private static final long serialVersionUID = 1L;

    private final JavaType type;
    private final Field valueField;
    private final Method getterMethod;

    /**
     * Cria um novo serializador para Value Objects de um único campo.
     *
     * @param type o tipo Java do Value Object
     */
    public SingleValueObjectSerializer(JavaType type) {
        this.type = type;
        this.valueField = findSingleField(type.getRawClass());
        this.getterMethod = findGetterMethod(type.getRawClass(), valueField);
        if (this.valueField != null) {
            this.valueField.setAccessible(true);
        }
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        try {
            Object fieldValue = getFieldValue(value);
            gen.writeObject(fieldValue);
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to access field for serialization", e);
        }
    }

    /**
     * Obtém o valor do campo do objeto.
     *
     * @param obj o objeto Value Object
     * @return o valor do campo
     */
    private Object getFieldValue(Object obj) throws IllegalAccessException {
        if (getterMethod != null) {
            try {
                return getterMethod.invoke(obj);
            } catch (Exception e) {
                // Fallback para campo direto
                if (valueField != null) {
                    return valueField.get(obj);
                }
            }
        }
        if (valueField != null) {
            return valueField.get(obj);
        }
        return null;
    }

    /**
     * Encontra o único campo da classe.
     *
     * @param clazz a classe a inspecionar
     * @return o campo encontrado
     */
    private Field findSingleField(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isSynthetic()) {
                return field;
            }
        }
        return null;
    }

    /**
     * Encontra o método getter para o campo.
     *
     * @param clazz a classe a inspecionar
     * @param field o campo
     * @return o método getter ou null se não existir
     */
    private Method findGetterMethod(Class<?> clazz, Field field) {
        if (field == null) {
            return null;
        }

        String fieldName = field.getName();
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // Tenta com "is" para boolean
            if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                String isMethodName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                try {
                    return clazz.getMethod(isMethodName);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }
            return null;
        }
    }
}
