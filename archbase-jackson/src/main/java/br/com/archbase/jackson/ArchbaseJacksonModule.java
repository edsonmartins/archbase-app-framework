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

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Módulo Jackson para serialização otimizada de Value Objects do Archbase.
 * <p>
 * Detecta automaticamente Value Objects com um único campo e os serializa
 * como o valor direto, sem wrapper JSON.
 * </p>
 * <p><b>Exemplo:</b></p>
 * <pre>
 * Sem módulo:  {"cpf": {"numero": "123.456.789-09"}}
 * Com módulo:   {"cpf": "123.456.789-09"}
 * </pre>
 *
 * <p>Para ativar este módulo, adicione ao ObjectMapper:</p>
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.registerModule(new ArchbaseJacksonModule());
 * }</pre>
 *
 * <p>Com Spring Boot e auto-configuração ativa, o módulo é registrado automaticamente
 * quando a propriedade {@code archbase.jackson.enabled} está {@code true}.</p>
 *
 * @see SingleValueObjectSerializer
 * @see ValueObjectDeserializer
 */
public class ArchbaseJacksonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    /**
     * Cria um novo módulo Archbase Jackson.
     */
    public ArchbaseJacksonModule() {
        super("ArchbaseJackson");

        // Registrar serializer para Value Objects
        setSerializerModifier(new ArchbaseBeanSerializerModifier());

        // Registrar deserializador para Value Objects
        setDeserializerModifier(new ArchbaseBeanDeserializerModifier());
    }
}
