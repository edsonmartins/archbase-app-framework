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
package br.com.archbase.identifier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.UUID;

/**
 * Desserializador Jackson para identificadores Archbase.
 * <p>
 * Suporta desserialização para tipos comuns de identificadores como
 * {@link UUID}, {@link Long}, e {@link String}.
 * </p>
 * <p>
 * Para tipos customizados, crie um desserializador específico
 * ou implemente o método estático {@code fromValue()} no seu tipo Identifier.
 * </p>
 *
 * @see Identifier
 * @see IdentifierModule
 */
public class IdentifierDeserializer extends JsonDeserializer<Identifier<?>> {

    @Override
    public Identifier<?> deserialize(JsonParser p, DeserializationContext context) throws IOException {
        String value = p.getValueAsString();

        if (value == null || value.isEmpty()) {
            return null;
        }

        // Tenta detectar o tipo pelo formato
        // UUID
        if (value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return new SimpleIdentifier<>(UUID.fromString(value));
        }

        // Long (números puros)
        if (value.matches("\\d+")) {
            return new SimpleIdentifier<>(Long.parseLong(value));
        }

        // String (padrão)
        return new SimpleIdentifier<>(value);
    }

    /**
     * Implementação simples de Identifier para tipos primitivos.
     *
     * @param <T> o tipo do valor
     */
    public static class SimpleIdentifier<T> implements Identifier<T> {

        private static final long serialVersionUID = 1L;

        private final T value;

        public SimpleIdentifier(T value) {
            this.value = value;
        }

        @Override
        public T getValue() {
            return value;
        }
    }
}
