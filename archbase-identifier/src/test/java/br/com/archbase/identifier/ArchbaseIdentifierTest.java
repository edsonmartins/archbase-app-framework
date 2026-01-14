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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes para o módulo archbase-identifier.
 */
class ArchbaseIdentifierTest {

    @Test
    void deveSerializarIdentifierComoValorDireto() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new IdentifierModule())
                .build();

        ClienteId id = new ClienteId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        String json = mapper.writeValueAsString(id);

        assertThat(json).isEqualTo("\"123e4567-e89b-12d3-a456-426614174000\"");
    }

    @Test
    void deveDesserializarValorParaIdentifier() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new IdentifierModule())
                .build();

        String json = "\"123e4567-e89b-12d3-a456-426614174000\"";

        Identifier<?> result = mapper.readValue(json, Identifier.class);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    void deveSerializarLongIdentifier() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new IdentifierModule())
                .build();

        LongId id = new LongId(12345L);

        String json = mapper.writeValueAsString(id);

        assertThat(json).isEqualTo("12345");
    }

    @Test
    void identifierModuleDeveTerNomeCorreto() {
        IdentifierModule module = new IdentifierModule();

        assertThat(module.getModuleName()).isEqualTo("ArchbaseIdentifier");
    }

    /**
     * Classe de teste para ClienteId.
     */
    static class ClienteId implements Identifier<UUID> {

        private static final long serialVersionUID = 1L;

        private final UUID value;

        public ClienteId(UUID value) {
            this.value = value;
        }

        @Override
        public UUID getValue() {
            return value;
        }
    }

    /**
     * Classe de teste para LongId.
     */
    static class LongId implements Identifier<Long> {

        private static final long serialVersionUID = 1L;

        private final Long value;

        public LongId(Long value) {
            this.value = value;
        }

        @Override
        public Long getValue() {
            return value;
        }
    }
}
