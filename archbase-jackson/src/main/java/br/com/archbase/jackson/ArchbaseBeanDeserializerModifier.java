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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Modificador de deserializador para Value Objects.
 * <p>
 * Permite desserializar Value Objects a partir do valor direto,
 * sem exigir um wrapper JSON.
 * </p>
 */
public class ArchbaseBeanDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyDeserializer(
            DeserializationConfig config,
            BeanDescription beanDesc,
            JsonDeserializer<?> deserializer) {

        Class<?> beanClass = beanDesc.getBeanClass();

        // Verifica se é um Value Object com um único campo
        if (isSingleFieldValueObject(beanClass)) {
            return new SingleValueObjectDeserializer(beanClass);
        }

        return deserializer;
    }

    /**
     * Verifica se a classe é um Value Object com um único campo.
     *
     * @param clazz a classe a inspecionar
     * @return true se for um Value Object com um único campo
     */
    private boolean isSingleFieldValueObject(Class<?> clazz) {
        // Verifica se implementa ValueObject (se disponível)
        if (isValueObject(clazz)) {
            return countNonSyntheticFields(clazz) == 1;
        }

        // Verifica se tem anotação @ValueObject
        if (hasValueObjectAnnotation(clazz)) {
            return countNonSyntheticFields(clazz) == 1;
        }

        return false;
    }

    /**
     * Conta campos não-sintéticos da classe.
     */
    private int countNonSyntheticFields(Class<?> clazz) {
        int count = 0;
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Verifica se a classe implementa a interface ValueObject.
     */
    @SuppressWarnings("unchecked")
    private boolean isValueObject(Class<?> clazz) {
        try {
            Class<?> valueObjectInterface = Class.forName("br.com.archbase.ddd.domain.valueobject.ValueObject");
            return valueObjectInterface.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Verifica se a classe tem anotação @ValueObject.
     */
    @SuppressWarnings("unchecked")
    private boolean hasValueObjectAnnotation(Class<?> clazz) {
        try {
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) Class.forName("br.com.archbase.ddd.domain.annotations.DomainValueObject");
            return clazz.getAnnotation(annotationClass) != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
