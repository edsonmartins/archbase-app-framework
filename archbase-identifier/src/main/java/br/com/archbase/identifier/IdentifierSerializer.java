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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializador Jackson para identificadores Archbase.
 * <p>
 * Serializa o valor do identificador diretamente, sem wrapper.
 * </p>
 * <p><b>Exemplo:</b></p>
 * <pre>
 * Sem serializador: {"id": {"value": "123e4567-e89b-12d3-a456-426614174000"}}
 * Com serializador: {"id": "123e4567-e89b-12d3-a456-426614174000"}
 * </pre>
 *
 * @see Identifier
 * @see IdentifierModule
 */
public class IdentifierSerializer extends JsonSerializer<Identifier<?>> {

    @Override
    public void serialize(Identifier<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            Object identifierValue = value.getValue();
            if (identifierValue == null) {
                gen.writeNull();
            } else {
                gen.writeObject(identifierValue);
            }
        }
    }
}
