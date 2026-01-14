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

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Módulo Jackson para serialização de identificadores Archbase.
 * <p>
 * Registra serializers e deserializers para tipos marcados com
 * {@link ArchbaseIdentifier} ou que implementam {@link Identifier}.
 * </p>
 * <p>
 * Para ativar este módulo, adicione ao ObjectMapper:
 * </p>
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.registerModule(new IdentifierModule());
 * }</pre>
 *
 * <p>Com Spring Boot e auto-configuração ativa, o módulo é registrado automaticamente.</p>
 *
 * @see ArchbaseIdentifier
 * @see Identifier
 * @see IdentifierSerializer
 * @see IdentifierDeserializer
 */
public class IdentifierModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    /**
     * Cria um novo módulo de identificador.
     */
    public IdentifierModule() {
        super("ArchbaseIdentifier");

        // Registrar serializer para Identifier
        @SuppressWarnings("unchecked")
        Class<Identifier<?>> identifierClass = (Class<Identifier<?>>) (Class<?>) Identifier.class;
        addSerializer(identifierClass, new IdentifierSerializer());

        // Registrar deserializer genérico
        addDeserializer(Identifier.class, new IdentifierDeserializer());
    }
}
